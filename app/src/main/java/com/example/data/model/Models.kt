package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

// ==========================================
// 1. GENERIC API RESPONSE ENVELOPES
// ==========================================

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null,
    val errors: Any? = null
)

@JsonClass(generateAdapter = true)
data class ApiBaseResponse(
    val success: Boolean = true,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    val success: Boolean = true,
    val message: String
)

@JsonClass(generateAdapter = true)
data class PaginationDto(
    @Json(name = "current_page")
    val currentPage: Int = 1,
    @Json(name = "last_page")
    val lastPage: Int = 1,
    @Json(name = "per_page")
    val perPage: Int = 20,
    val total: Int = 0
)

// ==========================================
// 2. AUTH & USER DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginDataDto(
    val token: String,
    val user: UserApiDto,
    @Json(name = "assigned_number")
    val assignedNumber: String? = null,
    @Json(name = "twilio_token")
    val twilioToken: String? = null,
    @Json(name = "twilio_token_ttl")
    val twilioTokenTtl: Long? = 3600
)

@JsonClass(generateAdapter = true)
data class MeDataDto(
    val user: UserApiDto,
    @Json(name = "assigned_number")
    val assignedNumber: String? = null,
    @Json(name = "twilio_token")
    val twilioToken: String? = null,
    @Json(name = "twilio_token_ttl")
    val twilioTokenTtl: Long? = 3600
)

@JsonClass(generateAdapter = true)
data class UserApiDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: String? = "user", // "admin" | "user"
    val status: String? = "active", // "active" | "inactive"
    @Json(name = "email_verified_at")
    val emailVerifiedAt: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val email: String
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

// ==========================================
// 3. PHONE NUMBER DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class PhoneNumberDataDto(
    @Json(name = "phone_number")
    val phoneNumber: PhoneNumberDetailDto? = null
)

@JsonClass(generateAdapter = true)
data class PhoneNumberDetailDto(
    val id: Long? = null,
    @Json(name = "phone_number")
    val phoneNumber: String, // +E.164
    @Json(name = "friendly_name")
    val friendlyName: String? = null,
    @Json(name = "country_code")
    val countryCode: String? = null,
    @Json(name = "iso_country")
    val isoCountry: String? = null,
    val capabilities: PhoneNumberCapabilitiesDto? = null,
    val status: String? = "assigned", // "available" | "assigned" | "releasing" | "released" | "failed"
    @Json(name = "purchased_at")
    val purchasedAt: String? = null,
    @Json(name = "assigned_at")
    val assignedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PhoneNumberCapabilitiesDto(
    val voice: Boolean = true,
    val sms: Boolean = true,
    val mms: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PhoneNumberResponse(
    @Json(name = "phone_number")
    val phoneNumber: String?,
    val status: String = "active",
    val country: String? = "US",
    @Json(name = "caller_name")
    val callerName: String? = null
)

// ==========================================
// 4. TWILIO TOKEN DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class TwilioTokenDataDto(
    val token: String,
    val ttl: Long? = 3600,
    val identity: String,
    @Json(name = "assigned_number")
    val assignedNumber: String? = null
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

// ==========================================
// 5. CALLS DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class CallsDataDto(
    val calls: List<CallApiDto> = emptyList(),
    val pagination: PaginationDto? = null
)

@JsonClass(generateAdapter = true)
data class CallDetailDataDto(
    val call: CallApiDto
)

@JsonClass(generateAdapter = true)
data class CallApiDto(
    val id: Long,
    @Json(name = "twilio_call_sid")
    val twilioCallSid: String? = null,
    val direction: String, // "incoming" | "outgoing"
    @Json(name = "from_number")
    val fromNumber: String,
    @Json(name = "to_number")
    val toNumber: String,
    val status: String, // "initiated" | "ringing" | "answered" | "completed" | "busy" | "failed" | "no-answer" | "canceled"
    val duration: Long? = 0,
    val price: String? = null, // e.g. "-0.0085"
    val currency: String? = null, // e.g. "USD"
    @Json(name = "started_at")
    val startedAt: String? = null,
    @Json(name = "answered_at")
    val answeredAt: String? = null,
    @Json(name = "ended_at")
    val endedAt: String? = null,
    @Json(name = "phone_number")
    val phoneNumber: PhoneNumberDetailDto? = null
)

// ==========================================
// 6. CONTACTS DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class ContactsDataDto(
    val contacts: List<ContactApiDto> = emptyList(),
    val pagination: PaginationDto? = null
)

@JsonClass(generateAdapter = true)
data class ContactDetailDataDto(
    val contact: ContactApiDto
)

@JsonClass(generateAdapter = true)
data class ContactApiDto(
    val id: Long,
    val name: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ContactCreateUpdateRequest(
    val name: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    val organization: String? = null
)

// ==========================================
// 7. DEVICES DTOs
// ==========================================

@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    @Json(name = "device_id")
    val deviceId: String,
    val platform: String = "android", // "ios" | "android"
    @Json(name = "push_token")
    val pushToken: String,
    @Json(name = "app_version")
    val appVersion: String? = "1.0.0"
)

@JsonClass(generateAdapter = true)
data class DeviceUnregisterRequest(
    @Json(name = "device_id")
    val deviceId: String
)

@JsonClass(generateAdapter = true)
data class DeviceDataDto(
    val device: DeviceDetailDto? = null
)

@JsonClass(generateAdapter = true)
data class DeviceDetailDto(
    val id: Long? = null,
    @Json(name = "user_id")
    val userId: Long? = null,
    @Json(name = "device_id")
    val deviceId: String,
    val platform: String,
    @Json(name = "push_token")
    val pushToken: String,
    @Json(name = "app_version")
    val appVersion: String? = null,
    @Json(name = "last_seen_at")
    val lastSeenAt: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

// ==========================================
// 8. DOMAIN MODELS (APP & UI LAYER)
// ==========================================

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    @Json(name = "phone_number")
    val assignedPhoneNumber: String? = null,
    val status: String = "active", // "active", "inactive", "suspended"
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    val company: String? = "BizVoice Global Corp",
    val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    @Json(name = "token_type")
    val tokenType: String = "Bearer",
    val user: User
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

// ==========================================
// 9. MAPPER EXTENSIONS
// ==========================================

fun parseIsoTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        Instant.parse(isoString).toEpochMilli()
    } catch (_: Exception) {
        try {
            val trimmed = if (isoString.contains(".")) {
                isoString.substringBefore(".") + "Z"
            } else isoString
            Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

fun UserApiDto.toUser(assignedNumber: String? = null): User {
    return User(
        id = id.toString(),
        name = name,
        email = email,
        assignedPhoneNumber = assignedNumber,
        status = status ?: "active",
        role = role ?: "user",
        company = "BizVoice"
    )
}

fun CallApiDto.toCallRecord(): CallRecord {
    val dir = when {
        status.equals("no-answer", ignoreCase = true) && direction.equals("incoming", ignoreCase = true) -> CallDirection.MISSED
        direction.equals("incoming", ignoreCase = true) -> CallDirection.INCOMING
        else -> CallDirection.OUTGOING
    }
    val recordStatus = when (status.lowercase()) {
        "completed", "answered" -> CallRecordStatus.COMPLETED
        "busy" -> CallRecordStatus.BUSY
        "no-answer" -> CallRecordStatus.NO_ANSWER
        "failed" -> CallRecordStatus.FAILED
        "canceled" -> CallRecordStatus.CANCELED
        else -> CallRecordStatus.COMPLETED
    }
    val remoteNum = if (direction.equals("incoming", ignoreCase = true)) fromNumber else toNumber
    return CallRecord(
        id = id.toString(),
        remotePhoneNumber = remoteNum,
        remoteName = null,
        direction = dir,
        durationSeconds = duration ?: 0,
        status = recordStatus,
        timestamp = parseIsoTimestamp(startedAt),
        twilioCallSid = twilioCallSid,
        notes = if (price != null) "Rate: $price ${currency ?: "USD"}" else null
    )
}

fun ContactApiDto.toContact(): Contact {
    return Contact(
        id = id.toString(),
        name = name,
        phoneNumber = phoneNumber,
        email = email,
        organization = null,
        isDeviceContact = false,
        createdAt = parseIsoTimestamp(createdAt)
    )
}

