package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

// =========================================================================
// 1. GENERIC API RESPONSE ENVELOPES (Growfone / Twilio Calling API v1.0)
// =========================================================================

@JsonClass(generateAdapter = true)
data class GrowfoneCodeEnvelope<T>(
    val code: Int? = 200,
    val data: T? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GrowfoneStatusBoolEnvelope<T>(
    val status: Boolean? = true,
    val data: T? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GrowfoneStatusIntEnvelope<T>(
    val status: Int? = 200,
    val data: T? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GrowfoneBaseMessageResponse(
    val code: Int? = null,
    val status: Boolean? = null,
    val message: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    val success: Boolean = true,
    val message: String
)

// =========================================================================
// 2. AUTH & USER DTOs (01 · Authentication & 02 · Profile & Account)
// =========================================================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class DeviceSignInRequest(
    val email: String,
    val password: String,
    @Json(name = "device_unique_id")
    val deviceUniqueId: String = "android_device_01",
    @Json(name = "device_token")
    val deviceToken: String = "",
    @Json(name = "device_type")
    val deviceType: String = "A", // "A" for Android, "I" for iOS
    @Json(name = "ip_address")
    val ipAddress: String = "0",
    @Json(name = "app_version")
    val appVersion: String = "1.0.0"
)

@JsonClass(generateAdapter = true)
data class CreateProfileRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    @Json(name = "phone_number")
    val phoneNumber: String? = null,
    @Json(name = "social_id")
    val socialId: String? = null,
    @Json(name = "new_login")
    val newLogin: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val token: String,
    val password: String,
    @Json(name = "password_confirmation")
    val passwordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val name: String? = null,
    @Json(name = "phone_number")
    val phoneNumber: String? = null,
    @Json(name = "social_id")
    val socialId: String? = null,
    @Json(name = "new_login")
    val newLogin: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "current_password")
    val currentPassword: String? = null,
    @Json(name = "new_password")
    val newPassword: String,
    @Json(name = "new_password_confirmation")
    val newPasswordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class GrowfoneUserDto(
    val id: String,
    val name: String? = "",
    val email: String = "",
    @Json(name = "email_verified_at")
    val emailVerifiedAt: String? = "",
    @Json(name = "phone_number")
    val phoneNumber: String? = "",
    @Json(name = "social_id")
    val socialId: String? = "",
    val credits: Int? = 0,
    @Json(name = "auth_key")
    val authKey: String? = "",
    @Json(name = "rpay_customer_id")
    val rpayCustomerId: String? = "",
    @Json(name = "profile_picture")
    val profilePicture: String? = "",
    @Json(name = "friendly_name")
    val friendlyName: String? = "",
    val status: String? = "active", // "active" | "inactive"
    @Json(name = "enable_reminder_notes")
    val enableReminderNotes: Boolean? = false,
    @Json(name = "enable_play_recording")
    val enablePlayRecording: Boolean? = false,
    @Json(name = "new_login")
    val newLogin: Boolean? = false,
    @Json(name = "parent_user_id")
    val parentUserId: String? = "",
    @Json(name = "role_id")
    val roleId: String? = "",
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminLoginDataDto(
    val user: GrowfoneUserDto,
    val token: String
)

@JsonClass(generateAdapter = true)
data class CreditsDataDto(
    val credits: Int = 0
)

@JsonClass(generateAdapter = true)
data class UserPermissionsDataDto(
    val user: GrowfoneUserDto? = null,
    val permissions: Map<String, PermissionModuleDto>? = emptyMap()
)

@JsonClass(generateAdapter = true)
data class PermissionModuleDto(
    val slug: String? = "",
    val actions: List<PermissionActionDto>? = emptyList(),
    @Json(name = "is_restricted")
    val isRestricted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PermissionActionDto(
    val id: String? = null,
    val action: String = "",
    @Json(name = "parent_id")
    val parentId: String? = null,
    @Json(name = "is_restricted")
    val isRestricted: Boolean = false
)

// =========================================================================
// 3. COMPANY DTOs (03 · Company)
// =========================================================================

@JsonClass(generateAdapter = true)
data class CompanyDto(
    val id: String? = null,
    @Json(name = "user_id")
    val userId: String? = null,
    val name: String = "",
    val email: String? = "",
    val website: String? = "",
    @Json(name = "mobile_number")
    val mobileNumber: String? = "",
    @Json(name = "gst_number")
    val gstNumber: String? = "",
    val address: String? = "",
    val city: String? = "",
    val state: String? = "",
    val country: String? = "",
    @Json(name = "postal_code")
    val postalCode: String? = "",
    @Json(name = "industry_type")
    val industryType: String? = "",
    val description: String? = "",
    val status: String? = "active",
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateCompanyRequest(
    val name: String,
    val email: String,
    val website: String? = null,
    @Json(name = "mobile_number")
    val mobileNumber: String? = null,
    @Json(name = "gst_number")
    val gstNumber: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @Json(name = "postal_code")
    val postalCode: String? = null,
    @Json(name = "industry_type")
    val industryType: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateCompanyRequest(
    @Json(name = "company_id")
    val companyId: String,
    val name: String? = null,
    val website: String? = null,
    @Json(name = "mobile_number")
    val mobileNumber: String? = null,
    @Json(name = "gst_number")
    val gstNumber: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @Json(name = "postal_code")
    val postalCode: String? = null,
    @Json(name = "industry_type")
    val industryType: String? = null,
    val description: String? = null,
    val status: String? = null
)

// =========================================================================
// 4. PHONE NUMBERS & CAPABILITY DTOs (04 · Phone Numbers & 05 · Calls)
// =========================================================================

@JsonClass(generateAdapter = true)
data class AreaListRequest(
    val country: String? = "US",
    val area: String? = ""
)

@JsonClass(generateAdapter = true)
data class AreaCodeDto(
    val id: Long? = null,
    val country: String? = "",
    val countryName: String? = "",
    val areaCode: String? = "",
    val region: String? = ""
)

@JsonClass(generateAdapter = true)
data class FindPhoneNumberRequest(
    val countryCode: String = "US",
    val areaCode: String? = ""
)

@JsonClass(generateAdapter = true)
data class AvailableNumberDto(
    val friendlyName: String = "",
    val phoneNumber: String = "" // E.164
)

@JsonClass(generateAdapter = true)
data class HoldNumberRequest(
    val phoneNumber: String,
    val friendlyName: String,
    val name: String,
    val email: String,
    val password: String,
    val status: String? = "active",
    @Json(name = "enable_reminder_notes")
    val enableReminderNotes: Boolean? = true,
    @Json(name = "enable_play_recording")
    val enablePlayRecording: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class HoldNumberDataDto(
    val id: String,
    val phoneNumber: String,
    val friendlyName: String,
    val isHold: Boolean = true,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseNumberRequest(
    val phoneNumber: String,
    val friendlyName: String,
    @Json(name = "user_id")
    val userId: String? = null
)

@JsonClass(generateAdapter = true)
data class PurchasedNumberDto(
    val id: String? = null,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "phone_number")
    val phoneNumber: String = "",
    @Json(name = "friendly_name")
    val friendlyName: String? = "",
    val sid: String? = null,
    @Json(name = "activation_date")
    val activationDate: Any? = null,
    val status: Int = 1, // 1 = Active, 9 = Marked for release
    @Json(name = "country_code")
    val countryCode: String? = "US",
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DialingCountryDto(
    @Json(name = "iso_code")
    val isoCode: String = "",
    val name: String = "",
    @Json(name = "calling_code")
    val callingCode: String = "",
    val continent: String? = null,
    val enabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CapabilityTokenDto(
    val identity: String,
    val token: String
)

@JsonClass(generateAdapter = true)
data class GetCallLogsRequest(
    val twilioNumber: String
)

@JsonClass(generateAdapter = true)
data class CallLogDto(
    @Json(name = "to_phone_number")
    val toPhoneNumber: String? = null,
    @Json(name = "from_phone_number")
    val fromPhoneNumber: String? = null,
    val startTime: String? = null,
    val duration: Long? = 0L,
    val direction: String? = null, // "outbound" | "inbound"
    val status: String? = null // "completed" | "no-answer" | "busy" | "failed" | "canceled" | "unknown"
)

// Legacy alias for phone number detail queries
@JsonClass(generateAdapter = true)
data class PhoneNumberResponse(
    @Json(name = "phone_number")
    val phoneNumber: String?,
    val status: String = "active",
    val country: String? = "US",
    @Json(name = "caller_name")
    val callerName: String? = null
)

// Legacy alias for Twilio token
@JsonClass(generateAdapter = true)
data class TwilioTokenResponse(
    val token: String,
    val identity: String,
    @Json(name = "expires_in")
    val expiresIn: Long = 3600,
    @Json(name = "account_sid")
    val accountSid: String? = null
)

// =========================================================================
// 5. CONTACTS DTOs (09 · Contacts)
// =========================================================================

@JsonClass(generateAdapter = true)
data class ContactsPagedDto(
    @Json(name = "current_page")
    val currentPage: Int = 1,
    val data: List<GrowfoneContactDto> = emptyList(),
    @Json(name = "first_page_url")
    val firstPageUrl: String? = null,
    val from: Int? = null,
    @Json(name = "last_page")
    val lastPage: Int = 1,
    @Json(name = "last_page_url")
    val lastPageUrl: String? = null,
    @Json(name = "next_page_url")
    val nextPageUrl: String? = null,
    val path: String? = null,
    @Json(name = "per_page")
    val perPage: Int = 15,
    @Json(name = "prev_page_url")
    val prevPageUrl: String? = null,
    val to: Int? = null,
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class GrowfoneContactDto(
    val id: Long = 0,
    @Json(name = "first_name")
    val firstName: String = "",
    @Json(name = "last_name")
    val lastName: String? = "",
    @Json(name = "full_name")
    val fullName: String? = "",
    val number: String = "",
    val extension: String? = "",
    val email: String? = "",
    @Json(name = "company_name")
    val companyName: String? = "",
    val notes: String? = "",
    val files: List<String>? = emptyList(),
    @Json(name = "is_dnd")
    val isDnd: Boolean = false,
    @Json(name = "is_blacklisted")
    val isBlacklisted: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateContactRequest(
    @Json(name = "first_name")
    val firstName: String,
    @Json(name = "last_name")
    val lastName: String? = null,
    val number: String,
    val extension: String? = null,
    val email: String? = null,
    @Json(name = "company_name")
    val companyName: String? = null,
    val notes: String? = null,
    val files: List<String>? = emptyList(),
    @Json(name = "is_dnd")
    val isDnd: Boolean? = false,
    @Json(name = "is_blacklisted")
    val isBlacklisted: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ToggleContactFlagRequest(
    val id: Long
)

// Legacy alias
@JsonClass(generateAdapter = true)
data class ContactCreateUpdateRequest(
    val name: String,
    @Json(name = "phone_number")
    val phoneNumber: String,
    val email: String? = null,
    val organization: String? = null
)

// =========================================================================
// 6. TEAM MEMBERS DTOs (06 · Team Members)
// =========================================================================

@JsonClass(generateAdapter = true)
data class TeamMembersPagedDto(
    @Json(name = "current_page")
    val currentPage: Int = 1,
    val data: List<GrowfoneUserDto> = emptyList(),
    @Json(name = "last_page")
    val lastPage: Int = 1,
    @Json(name = "per_page")
    val perPage: Int = 10,
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class CreateTeamMemberRequest(
    val name: String,
    val email: String,
    val password: String,
    @Json(name = "profile_picture")
    val profilePicture: String? = null,
    val status: String? = "active",
    @Json(name = "enable_reminder_notes")
    val enableReminderNotes: Boolean? = true,
    @Json(name = "enable_play_recording")
    val enablePlayRecording: Boolean? = true,
    @Json(name = "role_slug")
    val roleSlug: String? = "team-member",
    @Json(name = "role_id")
    val roleId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateTeamMemberRequest(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    @Json(name = "profile_picture")
    val profilePicture: String? = null,
    val status: String? = null,
    @Json(name = "role_id")
    val roleId: String? = null
)

// =========================================================================
// 7. DASHBOARD DTOs (07 · Dashboard)
// =========================================================================

@JsonClass(generateAdapter = true)
data class DashboardStatsDto(
    @Json(name = "total_calls")
    val totalCalls: Int = 0,
    @Json(name = "accepted_calls")
    val acceptedCalls: Int = 0,
    @Json(name = "rejected_calls")
    val rejectedCalls: Int = 0,
    @Json(name = "todays_outgoing_calls")
    val todaysOutgoingCalls: Int = 0,
    @Json(name = "todays_follow_ups")
    val todaysFollowUps: Int = 0,
    @Json(name = "total_team_members")
    val totalTeamMembers: Int = 0,
    @Json(name = "top_performers")
    val topPerformers: List<TopPerformerDto> = emptyList(),
    @Json(name = "top_dialed_countries")
    val topDialedCountries: List<TopDialedCountryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TopPerformerDto(
    val id: String,
    val name: String,
    @Json(name = "total_calls")
    val totalCalls: Int = 0
)

@JsonClass(generateAdapter = true)
data class TopDialedCountryDto(
    @Json(name = "to_country_code")
    val toCountryCode: String = "",
    @Json(name = "country_name")
    val countryName: String = "",
    @Json(name = "total_calls")
    val totalCalls: Int = 0
)

// =========================================================================
// 8. ROLES & PERMISSIONS DTOs (08 · Roles & Permissions)
// =========================================================================

@JsonClass(generateAdapter = true)
data class RoleDto(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "is_custom")
    val isCustom: Boolean = false,
    val permissions: List<RolePermissionItemDto>? = emptyList(),
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RolePermissionItemDto(
    val id: String,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRoleRequest(
    val name: String,
    val description: String? = null,
    @Json(name = "is_custom")
    val isCustom: Boolean = true,
    val permissions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateRoleRequest(
    val name: String? = null,
    val description: String? = null,
    val permissions: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AssignRoleBulkRequest(
    @Json(name = "role_id")
    val roleId: String,
    @Json(name = "user_ids")
    val userIds: List<String>
)

@JsonClass(generateAdapter = true)
data class RemoveRoleBulkRequest(
    @Json(name = "user_ids")
    val userIds: List<String>
)

@JsonClass(generateAdapter = true)
data class UpdateUserRoleRequest(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "role_id")
    val roleId: String
)

@JsonClass(generateAdapter = true)
data class UserRoleDetailDto(
    val user: GrowfoneUserDto? = null,
    val role: RoleDto? = null,
    val permissions: List<String> = emptyList()
)

// =========================================================================
// 9. FEEDBACK DTOs (10 · Feedback)
// =========================================================================

@JsonClass(generateAdapter = true)
data class FeedbackDto(
    val id: String,
    @Json(name = "user_id")
    val userId: String? = null,
    val title: String,
    val description: String,
    val type: String, // "bug" | "feature" | "improvement" | "other"
    val priority: String, // "low" | "medium" | "high" | "critical"
    val status: String = "pending", // "pending" | "in_progress" | "resolved" | "closed"
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,
    val user: GrowfoneUserDto? = null
)

@JsonClass(generateAdapter = true)
data class CreateFeedbackRequest(
    val title: String,
    val description: String,
    val type: String,
    val priority: String
)

@JsonClass(generateAdapter = true)
data class UpdateFeedbackRequest(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val priority: String? = null,
    val status: String? = null
)

// =========================================================================
// 10. PLANS & BILLING DTOs (11 · Plans & 12 · Billing & Payments)
// =========================================================================

@JsonClass(generateAdapter = true)
data class AllPlansDataDto(
    val credits: Int = 0,
    val subscription: List<PlanDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlanDto(
    val id: String,
    @Json(name = "product_id")
    val productId: String,
    val description: String? = "",
    val type: String = "subscription", // "subscription" | "credits"
    val credits: Int = 0,
    val price: Double = 0.0,
    @Json(name = "actual_price")
    val actualPrice: Double = 0.0,
    val discount: Double = 0.0,
    val percentage: Double = 0.0,
    val lists: List<String> = emptyList(),
    val caption: String? = "Subscribe Today",
    val labelAction: String? = "Choose Subscription"
)

@JsonClass(generateAdapter = true)
data class TeamSubscriptionsDataDto(
    @Json(name = "team_subscriptions")
    val teamSubscriptions: List<TeamSubscriptionItemDto> = emptyList(),
    val subscriptions: List<PlanDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TeamSubscriptionItemDto(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "team_name")
    val teamName: String = "",
    @Json(name = "phone_number")
    val phoneNumber: String? = "",
    @Json(name = "friendly_name")
    val friendlyName: String? = "",
    @Json(name = "current_subscription")
    val currentSubscription: CurrentSubscriptionDetailDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentSubscriptionDetailDto(
    @Json(name = "plan_id")
    val planId: String,
    @Json(name = "plan_name")
    val planName: String,
    @Json(name = "plan_type")
    val planType: String = "subscription",
    val status: String = "active",
    @Json(name = "start_date")
    val startDate: String? = "",
    @Json(name = "end_date")
    val endDate: String? = "",
    @Json(name = "days_remaining")
    val daysRemaining: Int = 0
)

@JsonClass(generateAdapter = true)
data class GeneratePaymentLinkRequest(
    @Json(name = "plan_id")
    val planId: String,
    @Json(name = "callback_url")
    val callbackUrl: String = "https://growfone.com/payment/callback",
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "hold_number_id")
    val holdNumberId: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckPaymentStatusRequest(
    @Json(name = "razorpay_payment_id")
    val razorpayPaymentId: String
)

@JsonClass(generateAdapter = true)
data class InvoicesDataDto(
    val user: GrowfoneUserDto? = null,
    val company: Any? = null, // Can be CompanyDto or empty string ""
    val invoices: List<InvoiceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InvoiceDto(
    val id: String,
    @Json(name = "razorpay_payment_id")
    val razorpayPaymentId: String? = "",
    val amount: Double = 0.0,
    val status: String = "success",
    @Json(name = "error_message")
    val errorMessage: String? = "",
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "plan_id")
    val planId: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

// =========================================================================
// 11. DOMAIN MODELS (APP & UI LAYER)
// =========================================================================

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
    val role: String? = "user",
    val credits: Int = 0,
    val authKey: String? = null
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
    @Json(name = "is_dnd")
    val isDnd: Boolean = false,
    @Json(name = "is_blacklisted")
    val isBlacklisted: Boolean = false,
    @Json(name = "notes")
    val notes: String? = null,
    @Json(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DialingCountry(
    val isoCode: String,
    val name: String,
    val callingCode: String,
    val continent: String? = null,
    val enabled: Boolean = true
) {
    val flagEmoji: String
        get() = isoCodeToEmoji(isoCode)

    companion object {
        fun isoCodeToEmoji(isoCode: String): String {
            val upper = isoCode.trim().uppercase()
            if (upper.length != 2) return "🌐"
            val first = upper[0]
            val second = upper[1]
            if (first !in 'A'..'Z' || second !in 'A'..'Z') return "🌐"
            val firstCode = 0x1F1E6 + (first - 'A')
            val secondCode = 0x1F1E6 + (second - 'A')
            return String(Character.toChars(firstCode)) + String(Character.toChars(secondCode))
        }
    }
}

// =========================================================================
// 12. MAPPER EXTENSIONS
// =========================================================================

fun parseIsoTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        Instant.parse(isoString).toEpochMilli()
    } catch (_: Exception) {
        try {
            val trimmed = if (isoString.contains(".")) {
                isoString.substringBefore(".") + "Z"
            } else if (isoString.contains(" ") && !isoString.contains("T")) {
                isoString.replace(" ", "T") + "Z"
            } else isoString
            Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

fun GrowfoneUserDto.toUser(assignedNumber: String? = null): User {
    val phone = if (!assignedNumber.isNullOrBlank()) assignedNumber else phoneNumber
    return User(
        id = id,
        name = name?.ifBlank { "User" } ?: "User",
        email = email,
        assignedPhoneNumber = phone?.ifBlank { null },
        status = status ?: "active",
        role = if (roleId?.isNotBlank() == true) "Team Member" else "user",
        company = "BizVoice Global Corp",
        credits = credits ?: 0,
        authKey = authKey
    )
}

fun CallLogDto.toCallRecord(index: Int = 0): CallRecord {
    val safeStatus = status?.lowercase()?.trim().orEmpty()
    val safeDirection = direction?.lowercase()?.trim() ?: "outbound"
    val safeTo = toPhoneNumber.orEmpty()
    val safeFrom = fromPhoneNumber.orEmpty()
    val safeDuration = duration ?: 0L

    val dir = when {
        safeStatus == "no-answer" && safeDirection == "inbound" -> CallDirection.MISSED
        safeDirection == "inbound" -> CallDirection.INCOMING
        else -> CallDirection.OUTGOING
    }
    val recordStatus = when (safeStatus) {
        "completed", "answered" -> CallRecordStatus.COMPLETED
        "busy" -> CallRecordStatus.BUSY
        "no-answer" -> CallRecordStatus.NO_ANSWER
        "failed", "unknown" -> CallRecordStatus.FAILED
        "canceled", "cancelled" -> CallRecordStatus.CANCELED
        "" -> CallRecordStatus.FAILED
        else -> CallRecordStatus.COMPLETED
    }
    val remoteNum = if (safeDirection == "inbound") {
        if (safeFrom.isNotBlank()) safeFrom else safeTo
    } else {
        if (safeTo.isNotBlank()) safeTo else safeFrom
    }
    val timestamp = parseIsoTimestamp(startTime)
    val rawKey = "${safeFrom}_${safeTo}_${startTime}_${safeDirection}_${safeDuration}_${safeStatus}_$index"
    val stableId = "log_" + (rawKey.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
    return CallRecord(
        id = stableId,
        remotePhoneNumber = remoteNum,
        remoteName = null,
        direction = dir,
        durationSeconds = safeDuration,
        status = recordStatus,
        timestamp = timestamp,
        twilioCallSid = null,
        notes = if (safeStatus.isNotBlank()) "Status: $safeStatus" else null
    )
}

fun GrowfoneContactDto.toContact(): Contact {
    val displayName = when {
        !fullName.isNullOrBlank() -> fullName
        !lastName.isNullOrBlank() -> "$firstName $lastName"
        else -> firstName
    }
    return Contact(
        id = id.toString(),
        name = displayName,
        phoneNumber = number,
        email = email?.ifBlank { null },
        organization = companyName?.ifBlank { null },
        isDeviceContact = false,
        isDnd = isDnd,
        isBlacklisted = isBlacklisted,
        notes = notes?.ifBlank { null },
        createdAt = parseIsoTimestamp(createdAt)
    )
}

fun DialingCountryDto.toDialingCountry(): DialingCountry {
    val formattedCode = if (callingCode.startsWith("+")) callingCode.trim() else "+${callingCode.trim()}"
    return DialingCountry(
        isoCode = isoCode.trim().uppercase(),
        name = name.trim(),
        callingCode = formattedCode,
        continent = continent?.trim(),
        enabled = enabled
    )
}
