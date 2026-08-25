package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.CapabilityTokenDto
import com.example.data.repository.BizVoiceRepository
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import com.twilio.voice.Call
import com.twilio.voice.CallException
import com.twilio.voice.ConnectOptions
import com.twilio.voice.Voice
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Structured disconnect/failure information with clean, user-friendly messages.
 */
data class DisconnectReason(
    val title: String,
    val userFriendlyMessage: String,
    val isNoAnswer: Boolean = false,
    val isBusy: Boolean = false,
    val isRejected: Boolean = false,
    val isNormalHangup: Boolean = false,
    val rawErrorCode: Int? = null
)

/**
 * Real Twilio Voice Android SDK VoIP Manager.
 * 
 * Complies with Twilio Voice Architecture:
 * 1. Originate call through Client Leg (client:<identity>) using an Access/Capability Token from backend.
 * 2. Never calls api.twilio.com REST API directly from mobile client.
 * 3. Passes destination number exclusively as custom parameter `To` (e.g. `To: "+918469620312"`).
 * 4. Never passes a `From` parameter (the backend derives caller ID from token identity and executes <Dial>).
 * 5. Caches Capability Token with safe TTL and refreshes before expiration.
 * 6. Uses com.twilio.voice.Voice.connect and com.twilio.audioswitch.AudioSwitch for real VoIP audio routing.
 */
class TwilioVoiceClientManager(
    private val context: Context,
    private val repository: BizVoiceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    companion object {
        private const val TAG = "TWILIO_VOICE_CLIENT"
        private const val TOKEN_TTL_MS = 50 * 60 * 1000L // 50 minutes safe cache TTL
    }

    // Twilio AudioSwitch for managing audio routing (Speakerphone, Earpiece, Bluetooth, Wired)
    private val audioSwitch: AudioSwitch = AudioSwitch(context.applicationContext, loggingEnabled = true)

    // Token Cache
    private var cachedTokenDto: CapabilityTokenDto? = null
    private var tokenFetchedAt: Long = 0L

    // Active Twilio Call & Session
    data class VoiceSession(
        val sessionId: String,
        val clientIdentity: String,
        val toPhoneNumber: String,
        val call: Call? = null,
        val startTime: Long = System.currentTimeMillis(),
        var hasRung: Boolean = false,
        var hasConnected: Boolean = false
    )

    private var activeSession: VoiceSession? = null
    private var activeTwilioCall: Call? = null

    // Callbacks for CallManager
    interface CallEventListener {
        fun onConnecting(sessionId: String)
        fun onRinging(sessionId: String)
        fun onConnected(sessionId: String)
        fun onReconnecting(sessionId: String)
        fun onDisconnected(sessionId: String, durationSeconds: Long, reason: DisconnectReason)
        fun onConnectFailure(sessionId: String, reason: DisconnectReason)
    }

    private var eventListener: CallEventListener? = null

    init {
        try {
            audioSwitch.start { audioDevices, selectedDevice ->
                Log.d(TAG, "AudioSwitch devices: ${audioDevices.map { it.name }}, selected: ${selectedDevice?.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioSwitch: ${e.message}", e)
        }
    }

    fun setEventListener(listener: CallEventListener?) {
        this.eventListener = listener
    }

    /**
     * Checks if cached capability token is still valid.
     */
    private fun isTokenValid(): Boolean {
        val token = cachedTokenDto?.token
        if (token.isNullOrBlank()) return false
        val age = System.currentTimeMillis() - tokenFetchedAt
        return age < TOKEN_TTL_MS
    }

    /**
     * Maps Twilio SDK exceptions and disconnect events to user-friendly messages.
     */
    private fun resolveDisconnectReason(
        error: CallException?,
        wasRinging: Boolean,
        wasConnected: Boolean,
        durationSeconds: Long
    ): DisconnectReason {
        if (error == null) {
            return when {
                durationSeconds > 0 || wasConnected -> DisconnectReason(
                    title = "Call Ended",
                    userFriendlyMessage = "Call completed",
                    isNormalHangup = true
                )
                wasRinging -> DisconnectReason(
                    title = "No Answer",
                    userFriendlyMessage = "The recipient did not answer the call.",
                    isNoAnswer = true
                )
                else -> DisconnectReason(
                    title = "Call Ended",
                    userFriendlyMessage = "Call was ended.",
                    isNormalHangup = true
                )
            }
        }

        val code = error.errorCode
        val msg = (error.message ?: "").lowercase()
        val exp = (error.explanation ?: "").lowercase()

        return when {
            // No Answer / Timeout
            code == 31603 || msg.contains("no answer") || msg.contains("no-answer") || exp.contains("no answer") || exp.contains("timeout") -> {
                DisconnectReason(
                    title = "No Answer",
                    userFriendlyMessage = "The recipient did not answer the call.",
                    isNoAnswer = true,
                    rawErrorCode = code
                )
            }
            // Line Busy
            code == 31486 || (code == 31600 && (msg.contains("busy") || exp.contains("busy"))) || msg.contains("busy") || exp.contains("busy") -> {
                DisconnectReason(
                    title = "Line Busy",
                    userFriendlyMessage = "The recipient is currently on another call. Please try again in a few moments.",
                    isBusy = true,
                    rawErrorCode = code
                )
            }
            // Rejected / Declined / Canceled
            code in listOf(31601, 31602, 31487) || msg.contains("decline") || msg.contains("reject") || exp.contains("decline") || exp.contains("reject") -> {
                DisconnectReason(
                    title = "Call Declined",
                    userFriendlyMessage = "The call was declined by the recipient.",
                    isRejected = true,
                    rawErrorCode = code
                )
            }
            // Invalid destination number / unallocated
            code in listOf(21211, 21214, 21217, 21219) || msg.contains("invalid number") || msg.contains("unallocated") || exp.contains("invalid number") -> {
                DisconnectReason(
                    title = "Invalid Number",
                    userFriendlyMessage = "The destination phone number is invalid. Please check the country code and digits.",
                    rawErrorCode = code
                )
            }
            // Geographic / International permission restrictions
            code in listOf(21408, 21421, 21422, 21401) || msg.contains("permission") || msg.contains("geo") || msg.contains("international") || exp.contains("permission") -> {
                DisconnectReason(
                    title = "Calling Restricted",
                    userFriendlyMessage = "Outbound calling to this destination country is restricted on your account.",
                    rawErrorCode = code
                )
            }
            // Insufficient funds / VoIP balance
            code in listOf(21210, 21212, 21614, 20003) || msg.contains("balance") || msg.contains("credit") || msg.contains("funds") || exp.contains("balance") -> {
                DisconnectReason(
                    title = "Insufficient Credits",
                    userFriendlyMessage = "Your account has insufficient balance to complete this call. Please recharge your credits.",
                    rawErrorCode = code
                )
            }
            // Auth token expiration / session invalid
            code in listOf(20101, 20104, 31201, 31204, 31205) || msg.contains("token") || msg.contains("jwt") || msg.contains("auth") -> {
                DisconnectReason(
                    title = "Session Expired",
                    userFriendlyMessage = "Your voice authorization token has expired. Please place the call again.",
                    rawErrorCode = code
                )
            }
            // Network / carrier unreachable
            code in listOf(31480, 31000, 31005, 31206, 31207) || msg.contains("network") || msg.contains("unavailable") || exp.contains("network") -> {
                DisconnectReason(
                    title = "Network Unavailable",
                    userFriendlyMessage = "Unable to connect to the telecommunications network. Please check your internet connection.",
                    rawErrorCode = code
                )
            }
            // Fallback: If it was ringing and duration is 0, it's a No Answer
            wasRinging && durationSeconds == 0L -> {
                DisconnectReason(
                    title = "No Answer",
                    userFriendlyMessage = "The recipient did not answer the call.",
                    isNoAnswer = true,
                    rawErrorCode = code
                )
            }
            else -> {
                DisconnectReason(
                    title = "Call Ended",
                    userFriendlyMessage = "Unable to complete the call. Please verify the number and try again.",
                    rawErrorCode = code
                )
            }
        }
    }

    /**
     * Retrieves or refreshes the Twilio Voice Access/Capability Token from the backend API.
     */
    suspend fun getOrFetchAccessToken(forceRefresh: Boolean = false): Result<CapabilityTokenDto> {
        if (!forceRefresh && isTokenValid() && cachedTokenDto != null) {
            return Result.success(cachedTokenDto!!)
        }

        val result = repository.getCapabilityToken(forceRefresh = forceRefresh)
        if (result.isSuccess) {
            val tokenData = result.getOrNull()
            if (tokenData != null && tokenData.token.isNotBlank()) {
                cachedTokenDto = tokenData
                tokenFetchedAt = System.currentTimeMillis()
                Log.i(TAG, "Successfully refreshed Voice Access Token for identity: ${tokenData.identity}")
                return Result.success(tokenData)
            }
        }
        return result
    }

    /**
     * Originates an outbound call using the real Twilio Voice SDK.
     */
    fun connectOutbound(toPhoneNumber: String, onResult: (Result<String>) -> Unit) {
        val cleanDestination = toPhoneNumber.trim()
        if (cleanDestination.length < 3) {
            onResult(Result.failure(IllegalArgumentException("Invalid destination phone number.")))
            return
        }

        // Verify runtime microphone permission BEFORE calling Voice.connect()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            val err = "Microphone permission is required to place calls."
            Log.e(TAG, "connectOutbound aborted: $err")
            onResult(Result.failure(SecurityException(err)))
            return
        }

        scope.launch {
            try {
                // Step 1: Ensure valid capability token from backend
                val tokenResult = getOrFetchAccessToken()
                if (tokenResult.isFailure) {
                    val err = tokenResult.exceptionOrNull()?.message ?: "Failed to obtain voice token from backend"
                    Log.e(TAG, "Cannot place call: $err")
                    onResult(Result.failure(Exception("Voice Authorization Error: $err")))
                    return@launch
                }

                val tokenData = tokenResult.getOrNull()!!
                val clientIdentity = tokenData.identity
                val sessionId = "client_call_" + UUID.randomUUID().toString().take(12)

                // Step 2: Build real Twilio ConnectOptions
                // Send "To" and ONLY "To". Never send "From".
                val params = mapOf("To" to cleanDestination)
                val connectOptions = ConnectOptions.Builder(tokenData.token)
                    .params(params)
                    .build()

                Log.i(TAG, "Connecting real Twilio Voice call: sessionId=$sessionId, clientIdentity=$clientIdentity, To=$cleanDestination")

                // Step 3: Implement real Call.Listener
                val callListener = object : Call.Listener {
                    override fun onConnectFailure(call: Call, error: CallException) {
                        Log.e(TAG, "Twilio Call.Listener onConnectFailure: code=${error.errorCode}, message=${error.message}, explanation=${error.explanation}")
                        val session = activeSession
                        val wasRinging = session?.hasRung == true
                        val wasConnected = session?.hasConnected == true
                        
                        stopAudioRouting()
                        activeTwilioCall = null
                        activeSession = null

                        val reason = resolveDisconnectReason(
                            error = error,
                            wasRinging = wasRinging,
                            wasConnected = wasConnected,
                            durationSeconds = 0L
                        )
                        eventListener?.onConnectFailure(sessionId, reason)
                    }

                    override fun onRinging(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onRinging: callSid=${call.sid}")
                        activeSession?.hasRung = true
                        eventListener?.onRinging(sessionId)
                    }

                    override fun onConnected(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onConnected: callSid=${call.sid}, from=${call.from}, to=${call.to}")
                        activeSession?.hasConnected = true
                        activeSession = activeSession?.copy(startTime = System.currentTimeMillis())
                        eventListener?.onConnected(sessionId)
                    }

                    override fun onReconnecting(call: Call, error: CallException) {
                        Log.w(TAG, "Twilio Call.Listener onReconnecting: code=${error.errorCode}, message=${error.message}")
                        eventListener?.onReconnecting(sessionId)
                    }

                    override fun onReconnected(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onReconnected: callSid=${call.sid}")
                        eventListener?.onConnected(sessionId)
                    }

                    override fun onDisconnected(call: Call, error: CallException?) {
                        if (error != null) {
                            Log.w(TAG, "Twilio Call.Listener onDisconnected with error: code=${error.errorCode}, message=${error.message}")
                        } else {
                            Log.i(TAG, "Twilio Call.Listener onDisconnected: callSid=${call.sid}")
                        }
                        
                        val session = activeSession
                        val duration = session?.let { (System.currentTimeMillis() - it.startTime) / 1000 } ?: 0L
                        val wasRinging = session?.hasRung == true
                        val wasConnected = session?.hasConnected == true

                        stopAudioRouting()
                        activeTwilioCall = null
                        activeSession = null

                        val reason = resolveDisconnectReason(
                            error = error,
                            wasRinging = wasRinging,
                            wasConnected = wasConnected,
                            durationSeconds = duration
                        )
                        eventListener?.onDisconnected(sessionId, duration, reason)
                    }
                }

                // Step 4: Activate audio and invoke Voice.connect()
                startAudioRouting()
                val twilioCall = Voice.connect(context, connectOptions, callListener)
                activeTwilioCall = twilioCall
                activeSession = VoiceSession(
                    sessionId = sessionId,
                    clientIdentity = clientIdentity,
                    toPhoneNumber = cleanDestination,
                    call = twilioCall,
                    startTime = System.currentTimeMillis()
                )

                // Notify caller that call initiation was accepted by SDK
                onResult(Result.success(sessionId))
                eventListener?.onConnecting(sessionId)

            } catch (e: Exception) {
                Log.e(TAG, "Error initiating real Twilio Voice call: ${e.message}", e)
                stopAudioRouting()
                activeTwilioCall = null
                activeSession = null
                onResult(Result.failure(e))
                eventListener?.onConnectFailure(
                    "client_call_init",
                    DisconnectReason(
                        title = "Call Ended",
                        userFriendlyMessage = e.message ?: "Unable to establish VoIP connection."
                    )
                )
            }
        }
    }

    /**
     * Disconnects the active Twilio Call.
     */
    fun disconnect(sessionId: String? = null) {
        try {
            activeTwilioCall?.disconnect()
            Log.i(TAG, "activeTwilioCall.disconnect() invoked")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect active Twilio Call: ${e.message}", e)
        }
        stopAudioRouting()
        activeTwilioCall = null
        val session = activeSession
        activeSession = null
        if (session != null) {
            val duration = (System.currentTimeMillis() - session.startTime) / 1000
            val reason = if (duration > 0 || session.hasConnected) {
                DisconnectReason(title = "Call Ended", userFriendlyMessage = "Call ended", isNormalHangup = true)
            } else {
                DisconnectReason(title = "Call Canceled", userFriendlyMessage = "Call was canceled", isNormalHangup = true)
            }
            eventListener?.onDisconnected(session.sessionId, maxOf(0L, duration), reason)
        }
    }

    /**
     * Controls real WebRTC audio stream mute via Twilio Call object.
     */
    fun setMuted(isMuted: Boolean) {
        try {
            activeTwilioCall?.mute(isMuted)
            Log.d(TAG, "Twilio Call mute set to: $isMuted (callSid=${activeTwilioCall?.sid})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle Twilio Call mute: ${e.message}")
        }
    }

    /**
     * Controls speakerphone routing via Twilio AudioSwitch.
     */
    fun setSpeakerphoneOn(isOn: Boolean) {
        try {
            val available = audioSwitch.availableAudioDevices
            val targetDevice = if (isOn) {
                available.firstOrNull { it is AudioDevice.Speakerphone }
            } else {
                available.firstOrNull { it is AudioDevice.Earpiece || it is AudioDevice.BluetoothHeadset || it is AudioDevice.WiredHeadset }
            }
            if (targetDevice != null) {
                audioSwitch.selectDevice(targetDevice)
                Log.d(TAG, "AudioSwitch selected device: ${targetDevice.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle speakerphone in AudioSwitch: ${e.message}")
        }
    }

    private fun startAudioRouting() {
        try {
            audioSwitch.activate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate AudioSwitch: ${e.message}")
        }
    }

    private fun stopAudioRouting() {
        try {
            audioSwitch.deactivate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate AudioSwitch: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            activeTwilioCall?.disconnect()
            activeTwilioCall = null
            audioSwitch.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup exception: ${e.message}")
        }
    }
}
