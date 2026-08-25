package com.example.data.remote

import android.content.Context
import com.example.data.local.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val sessionManager: SessionManager) {

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        val jwtToken = sessionManager.getAuthToken()
        if (!jwtToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $jwtToken")
        }

        val deviceAuth = sessionManager.getDeviceAuthKey()
        if (deviceAuth.isNotBlank() && deviceAuth != "device_auth_key_default") {
            builder.header("authToken", deviceAuth)
            builder.header("auth-token", deviceAuth)
            builder.header("auth_token", deviceAuth)
            builder.header("token", deviceAuth)
            builder.header("authKey", deviceAuth)
            builder.header("auth_key", deviceAuth)
        } else if (!jwtToken.isNullOrBlank()) {
            builder.header("authToken", jwtToken)
            builder.header("auth-token", jwtToken)
        }

        val secretKey = sessionManager.getSecretKey().trim()
        if (secretKey.isNotBlank()) {
            builder.header("secretKey", secretKey)
            builder.header("secret-key", secretKey)
        }

        builder.header("Accept", "application/json")
        builder.header("X-App-Platform", "Android")

        chain.proceed(builder.build())
    }

    private val unauthorizedInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        val isAuthEndpoint = path.contains("admin/login") || path.contains("sign-in") || path.contains("create-profile") || path.contains("password/forgot") || path.contains("password/reset")

        if (!isAuthEndpoint && sessionManager.isLoggedIn()) {
            var isUnauthorized = response.code == 401 || response.code == 403
            if (!isUnauthorized) {
                try {
                    val peekBody = response.peekBody(2048).string()
                    if (peekBody.contains("Auth Token is expired", ignoreCase = true) ||
                        peekBody.contains("Auth token can not be empty", ignoreCase = true) ||
                        peekBody.contains("\"status\":401") ||
                        peekBody.contains("\"code\":401") ||
                        peekBody.contains("Unauthenticated.", ignoreCase = true)
                    ) {
                        isUnauthorized = true
                    }
                } catch (_: Exception) {}
            }

            if (isUnauthorized) {
                sessionManager.notifyUnauthorized("Your session has expired. Please log in again.")
            }
        }
        response
    }

    private val twilioWebhookLoggingInterceptor = TwilioWebhookLoggingInterceptor()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(unauthorizedInterceptor)
        .addInterceptor(twilioWebhookLoggingInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getService(): LaravelApiService {
        var rawUrl = sessionManager.baseApiUrl.trim()
        if (!rawUrl.endsWith("/")) {
            rawUrl = "$rawUrl/"
        }
        val baseUrl = if (!rawUrl.contains("/api/v1") && !rawUrl.contains("/api/")) {
            "${rawUrl}api/v1.0/"
        } else {
            rawUrl
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LaravelApiService::class.java)
    }
}
