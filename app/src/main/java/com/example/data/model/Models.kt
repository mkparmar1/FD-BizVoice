package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    @Json(name = "phone_number")
    val assignedPhoneNumber: String? = null,
    val status: String = "active", // "active", "suspended", "unassigned"
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    val company: String? = "BizVoice Global Corp",
    val role: String? = "Agent"
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    @Json(name = "token_type")
    val tokenType: String = "Bearer",
    val user: User
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    val success: Boolean = true,
    val message: String
)

@JsonClass(generateAdapter = true)
data class PhoneNumberResponse(
    @Json(name = "phone_number")
    val phoneNumber: String?,
    val status: String = "active", // "active", "released", "unavailable"
    val country: String? = "US",
    @Json(name = "caller_name")
    val callerName: String? = null
)

@JsonClass(generateAdapter = true)
data class TwilioTokenResponse(
    val token: String,
    val identity: String,
    @Json(name = "expires_in")
    val expiresIn: Long = 3600,
    @Json(name = "account_sid")
    val accountSid: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    @Json(name = "device_id")
    val deviceId: String,
    val platform: String = "android",
    @Json(name = "push_token")
    val pushToken: String,
    @Json(name = "app_version")
    val appVersion: String
)

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallRecordStatus {
    COMPLETED,
    BUSY,
    NO_ANSWER,
    FAILED,
    CANCELED
}

@JsonClass(generateAdapter = true)
data class CallRecord(
    val id: String,
    @Json(name = "remote_phone_number")
    val remotePhoneNumber: String,
    @Json(name = "remote_name")
    val remoteName: String? = null,
    val direction: CallDirection,
    @Json(name = "duration_seconds")
    val durationSeconds: Long = 0,
    val status: CallRecordStatus = CallRecordStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "twilio_call_sid")
    val twilioCallSid: String? = null,
    val notes: String? = null,
    @Json(name = "is_recorded")
    val isRecorded: Boolean = false,
    @Json(name = "recording_duration_seconds")
    val recordingDurationSeconds: Long = 0,
    @Json(name = "recording_url")
    val recordingUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class Contact(
    val id: String,
    val name: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    val organization: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "is_device_contact")
    val isDeviceContact: Boolean = false,
    @Json(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ContactCreateUpdateRequest(
    val name: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    val organization: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val name: String,
    val email: String,
    @Json(name = "phone_number")
    val assignedPhoneNumber: String? = null,
    val company: String? = null,
    val role: String? = null,
    val status: String? = null
)
