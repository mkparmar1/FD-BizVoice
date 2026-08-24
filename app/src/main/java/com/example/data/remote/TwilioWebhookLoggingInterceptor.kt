package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time logging interceptor for Retrofit/OkHttp to monitor and troubleshoot
 * communication between the app and Twilio/Laravel voice webhooks.
 */
class TwilioWebhookLoggingInterceptor : Interceptor {

    data class NetworkLogEntry(
        val id: Long = System.currentTimeMillis(),
        val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
        val method: String,
        val url: String,
        val endpoint: String,
        val isVoiceOrWebhook: Boolean,
        val requestHeaders: Map<String, String>,
        val requestBody: String?,
        val statusCode: Int? = null,
        val statusMessage: String? = null,
        val durationMs: Long? = null,
        val responseBody: String? = null,
        val diagnosticMessage: String? = null,
        val isSuccess: Boolean = false
    )

    companion object {
        private const val TAG = "TWILIO_VOICE_LOGGER"
        private const val MAX_BODY_PEEK = 64 * 1024L // 64 KB

        private val _recentLogs = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
        val recentLogs: StateFlow<List<NetworkLogEntry>> = _recentLogs.asStateFlow()

        private fun addLog(entry: NetworkLogEntry) {
            val current = _recentLogs.value.toMutableList()
            if (current.size >= 100) {
                current.removeAt(0)
            }
            current.add(entry)
            _recentLogs.value = current
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val urlString = request.url.toString()
        val path = request.url.encodedPath
        val method = request.method

        val isVoiceOrWebhook = isTelephonyOrWebhookRoute(path, urlString)
        val startTime = System.currentTimeMillis()

        // Extract Request Headers (masking sensitive tokens partially for security while showing presence)
        val requestHeaders = mutableMapOf<String, String>()
        for (i in 0 until request.headers.size) {
            val name = request.headers.name(i)
            val value = request.headers.value(i)
            requestHeaders[name] = if (name.equals("Authorization", ignoreCase = true) && value.length > 20) {
                value.take(15) + "..." + value.takeLast(6)
            } else {
                value
            }
        }

        // Read Request Body if present
        val requestBodyStr = readRequestBody(request)

        val logPrefix = if (isVoiceOrWebhook) "[VOICE/WEBHOOK]" else "[API]"
        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "$logPrefix >>> $method $urlString")
        if (isVoiceOrWebhook) {
            Log.i(TAG, "$logPrefix Auth Headers: ${requestHeaders.filterKeys { it.contains("auth", true) || it.contains("token", true) || it.contains("secret", true) }}")
            if (!requestBodyStr.isNullOrBlank()) {
                Log.d(TAG, "$logPrefix Request Body: $requestBodyStr")
            }
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = "Network connection failure: ${e.message}"
            Log.e(TAG, "$logPrefix <<< $method $urlString FAILED in ${duration}ms: $errorMsg", e)

            addLog(
                NetworkLogEntry(
                    method = method,
                    url = urlString,
                    endpoint = path,
                    isVoiceOrWebhook = isVoiceOrWebhook,
                    requestHeaders = requestHeaders,
                    requestBody = requestBodyStr,
                    durationMs = duration,
                    diagnosticMessage = errorMsg,
                    isSuccess = false
                )
            )
            throw e
        }

        val duration = System.currentTimeMillis() - startTime
        val code = response.code
        val message = response.message

        // Safely peek response body without consuming the stream
        val responseBodyStr = try {
            response.peekBody(MAX_BODY_PEEK).string()
        } catch (_: Exception) {
            null
        }

        // Analyze and diagnose common voice / webhook triggering issues
        val diagnosticMsg = diagnoseVoiceResponse(path, code, responseBodyStr)

        if (code in 200..299) {
            Log.i(TAG, "$logPrefix <<< $code $message ($duration ms) from $urlString")
            if (isVoiceOrWebhook && !responseBodyStr.isNullOrBlank()) {
                Log.d(TAG, "$logPrefix Response Payload: $responseBodyStr")
            }
        } else {
            Log.w(TAG, "$logPrefix <<< HTTP $code $message ($duration ms) on $urlString")
            if (!responseBodyStr.isNullOrBlank()) {
                Log.w(TAG, "$logPrefix Error Response Body: $responseBodyStr")
            }
            if (diagnosticMsg != null) {
                Log.e(TAG, "$logPrefix DIAGNOSTIC: $diagnosticMsg")
            }
        }
        Log.i(TAG, "--------------------------------------------------")

        addLog(
            NetworkLogEntry(
                method = method,
                url = urlString,
                endpoint = path,
                isVoiceOrWebhook = isVoiceOrWebhook,
                requestHeaders = requestHeaders,
                requestBody = requestBodyStr,
                statusCode = code,
                statusMessage = message,
                durationMs = duration,
                responseBody = responseBodyStr,
                diagnosticMessage = diagnosticMsg,
                isSuccess = code in 200..299
            )
        )

        return response
    }

    private fun isTelephonyOrWebhookRoute(path: String, fullUrl: String): Boolean {
        val lower = (path + fullUrl).lowercase()
        return lower.contains("getcapabilitytoken") ||
                lower.contains("webhooks/twilio") ||
                lower.contains("voice/status") ||
                lower.contains("voice/received") ||
                lower.contains("voice/outbound") ||
                lower.contains("voice/inbound") ||
                lower.contains("sms/status") ||
                lower.contains("getcalllogs") ||
                lower.contains("getnumberdetails") ||
                lower.contains("twilio")
    }

    private fun readRequestBody(request: Request): String? {
        return try {
            val copy = request.newBuilder().build()
            val body = copy.body ?: return null
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readString(StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun diagnoseVoiceResponse(path: String, code: Int, body: String?): String? {
        val lowerPath = path.lowercase()
        if (lowerPath.contains("getcapabilitytoken")) {
            return when (code) {
                401 -> "Capability Token rejected (401 Unauthorized). Twilio cannot connect and will not fire voice webhooks (outbound/status). Check Laravel JWT or authKey middleware."
                404 -> "Capability Token endpoint /getCapabilityToken not found (404). Check route definitions in routes/api.php."
                500 -> "Laravel server threw 500 while generating Twilio capability token. Check TWILIO_ACCOUNT_SID, TWILIO_API_KEY, TWILIO_TWIML_APP_SID in .env."
                200 -> if (body != null && (!body.contains("token") || body.contains("\"status\":false"))) {
                    "Server returned 200 but token payload is missing or status=false. Verify Laravel response format."
                } else null
                else -> null
            }
        }

        if (lowerPath.contains("webhooks/twilio")) {
            return when (code) {
                404 -> "Twilio webhook route $path not found (404). Verify Twilio Console URL configuration."
                500 -> "Twilio webhook handler crashed with 500 error on server."
                else -> null
            }
        }

        return null
    }
}
