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

        val token = sessionManager.getAuthToken()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        val deviceAuth = sessionManager.getDeviceAuthKey()
        if (deviceAuth.isNotBlank()) {
            builder.header("authToken", deviceAuth)
            builder.header("auth-token", deviceAuth)
            builder.header("authKey", deviceAuth)
            builder.header("auth_key", deviceAuth)
        }
        val secretKey = sessionManager.getSecretKey()
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
        if (response.code == 401) {
            val path = request.url.encodedPath
            val isAuthEndpoint = path.contains("admin/login") || path.contains("sign-in") || path.contains("create-profile")
            if (!isAuthEndpoint && sessionManager.isLoggedIn()) {
                sessionManager.notifyUnauthorized("Session expired or unauthorized (401). Please log in again.")
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
