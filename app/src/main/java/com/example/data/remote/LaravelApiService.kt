package com.example.data.remote

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LaravelApiService {

    // =========================================================================
    // 1. AUTH & SESSIONS (01 · Authentication)
    // =========================================================================

    @POST("admin/login")
    suspend fun adminLogin(@Body req: LoginRequest): Response<GrowfoneCodeEnvelope<AdminLoginDataDto>>

    @POST("SignIn")
    suspend fun signIn(@Body req: DeviceSignInRequest): Response<GrowfoneStatusIntEnvelope<GrowfoneUserDto>>

    @POST("createProfile")
    suspend fun createProfile(@Body req: CreateProfileRequest): Response<GrowfoneStatusBoolEnvelope<GrowfoneUserDto>>

    @POST("logout")
    suspend fun logout(): Response<GrowfoneCodeEnvelope<Map<String, Any>>>

    @POST("password/forgot")
    suspend fun forgotPassword(@Body req: ForgotPasswordRequest): Response<GrowfoneStatusBoolEnvelope<Any?>>

    @POST("password/reset")
    suspend fun resetPassword(@Body req: ResetPasswordRequest): Response<GrowfoneStatusBoolEnvelope<Any?>>

    // =========================================================================
    // 2. PROFILE & ACCOUNT (02 · Profile & Account)
    // =========================================================================

    @GET("getProfile")
    suspend fun getProfile(): Response<GrowfoneCodeEnvelope<GrowfoneUserDto>>

    @POST("updateProfile")
    suspend fun updateProfile(@Body req: UpdateProfileRequest): Response<GrowfoneCodeEnvelope<GrowfoneUserDto>>

    @POST("changePassword")
    suspend fun changePassword(@Body req: ChangePasswordRequest): Response<GrowfoneCodeEnvelope<Map<String, Any>>>

    @GET("getCredit")
    suspend fun getCredit(): Response<GrowfoneStatusIntEnvelope<CreditsDataDto>>

    @GET("user-permissions")
    suspend fun getUserPermissions(): Response<GrowfoneCodeEnvelope<UserPermissionsDataDto>>

    // =========================================================================
    // 3. COMPANY (03 · Company)
    // =========================================================================

    @GET("companies")
    suspend fun getCompany(): Response<GrowfoneStatusBoolEnvelope<CompanyDto>>

    @POST("companies")
    suspend fun createCompany(@Body req: CreateCompanyRequest): Response<GrowfoneStatusBoolEnvelope<CompanyDto>>

    @POST("companyUpdate")
    suspend fun updateCompany(@Body req: UpdateCompanyRequest): Response<GrowfoneStatusBoolEnvelope<CompanyDto>>

    // =========================================================================
    // 4. PHONE NUMBERS & TELEPHONY (04 · Phone Numbers & 05 · Calls)
    // =========================================================================

    @POST("areaList")
    suspend fun getAreaList(@Body req: AreaListRequest): Response<GrowfoneStatusIntEnvelope<List<AreaCodeDto>>>

    @POST("findPhoneNumber")
    suspend fun findPhoneNumber(
        @Header("secretKey") secretKey: String,
        @Body req: FindPhoneNumberRequest
    ): Response<GrowfoneStatusIntEnvelope<List<AvailableNumberDto>>>

    @POST("holdNumber")
    suspend fun holdNumber(
        @Header("secretKey") secretKey: String,
        @Body req: HoldNumberRequest
    ): Response<GrowfoneStatusIntEnvelope<HoldNumberDataDto>>

    @POST("purchaseNumbers")
    suspend fun purchaseNumbers(@Body req: PurchaseNumberRequest): Response<GrowfoneStatusIntEnvelope<PurchasedNumberDto>>

    @GET("getNumberDetails")
    suspend fun getNumberDetails(@Header("authToken") authToken: String): Response<GrowfoneStatusIntEnvelope<PurchasedNumberDto>>

    @GET("getAllNumberDetails")
    suspend fun getAllNumberDetails(@Header("authToken") userUuid: String): Response<GrowfoneStatusIntEnvelope<List<PurchasedNumberDto>>>

    @GET("getCapabilityToken")
    suspend fun getCapabilityToken(@Header("authToken") authToken: String): Response<GrowfoneStatusIntEnvelope<CapabilityTokenDto>>

    @GET("getDialingCountries")
    suspend fun getDialingCountries(@Header("authToken") authToken: String): Response<GrowfoneStatusIntEnvelope<List<DialingCountryDto>>>

    @POST("getCallLogs")
    suspend fun getCallLogs(
        @Header("authToken") authToken: String,
        @Body req: GetCallLogsRequest
    ): Response<GrowfoneStatusIntEnvelope<List<CallLogDto>>>

    // =========================================================================
    // 5. CONTACTS (09 · Contacts)
    // =========================================================================

    @GET("contacts/list")
    suspend fun getContacts(
        @Query("per_page") perPage: Int? = 15,
        @Query("page") page: Int? = 1,
        @Query("name") name: String? = null,
        @Query("email") email: String? = null,
        @Query("company") company: String? = null,
        @Query("number") number: String? = null,
        @Query("is_dnd") isDnd: Int? = null,
        @Query("is_blacklisted") isBlacklisted: Int? = null
    ): Response<GrowfoneCodeEnvelope<ContactsPagedDto>>

    @POST("contacts/new")
    suspend fun createContact(@Body req: CreateContactRequest): Response<GrowfoneCodeEnvelope<GrowfoneContactDto>>

    @GET("contacts/get/{id}")
    suspend fun getContact(@Path("id") id: Long): Response<GrowfoneCodeEnvelope<GrowfoneContactDto>>

    @POST("contacts/update/{id}")
    suspend fun updateContact(
        @Path("id") id: Long,
        @Body req: CreateContactRequest
    ): Response<GrowfoneCodeEnvelope<GrowfoneContactDto>>

    @DELETE("contacts/delete/{id}")
    suspend fun deleteContact(@Path("id") id: Long): Response<GrowfoneCodeEnvelope<Map<String, Any>>>

    @GET("contacts/query")
    suspend fun queryContacts(
        @Query("per_page") perPage: Int? = 15,
        @Query("page") page: Int? = 1,
        @Query("name") name: String? = null,
        @Query("email") email: String? = null,
        @Query("company") company: String? = null,
        @Query("number") number: String? = null,
        @Query("is_dnd") isDnd: Int? = null,
        @Query("is_blacklisted") isBlacklisted: Int? = null
    ): Response<GrowfoneCodeEnvelope<ContactsPagedDto>>

    @POST("contacts/toggle_dnd")
    suspend fun toggleContactDnd(@Body req: ToggleContactFlagRequest): Response<GrowfoneCodeEnvelope<GrowfoneContactDto>>

    @POST("contacts/toggle_blacklist")
    suspend fun toggleContactBlacklist(@Body req: ToggleContactFlagRequest): Response<GrowfoneCodeEnvelope<GrowfoneContactDto>>

    // =========================================================================
    // 6. TEAM MEMBERS (06 · Team Members)
    // =========================================================================

    @GET("getAllTeamMembers")
    suspend fun getAllTeamMembers(
        @Query("per_page") perPage: Int? = 10,
        @Query("page") page: Int? = 1
    ): Response<GrowfoneStatusBoolEnvelope<TeamMembersPagedDto>>

    @POST("createTeamMember")
    suspend fun createTeamMember(@Body req: CreateTeamMemberRequest): Response<GrowfoneStatusBoolEnvelope<GrowfoneUserDto>>

    @POST("updateTeamMember")
    suspend fun updateTeamMember(@Body req: UpdateTeamMemberRequest): Response<GrowfoneStatusBoolEnvelope<GrowfoneUserDto>>

    @DELETE("deleteTeamMember/{id}")
    suspend fun deleteTeamMember(@Path("id") id: String): Response<GrowfoneStatusBoolEnvelope<Any?>>

    // =========================================================================
    // 7. DASHBOARD (07 · Dashboard)
    // =========================================================================

    @GET("dashboard")
    suspend fun getDashboard(): Response<GrowfoneStatusBoolEnvelope<DashboardStatsDto>>

    // =========================================================================
    // 8. ROLES & PERMISSIONS (08 · Roles & Permissions)
    // =========================================================================

    @GET("roles")
    suspend fun getRoles(): Response<GrowfoneStatusBoolEnvelope<List<RoleDto>>>

    @POST("roles")
    suspend fun createRole(@Body req: CreateRoleRequest): Response<GrowfoneStatusBoolEnvelope<RoleDto>>

    @PUT("roles/{id}")
    suspend fun updateRole(
        @Path("id") id: String,
        @Body req: UpdateRoleRequest
    ): Response<GrowfoneStatusBoolEnvelope<RoleDto>>

    @DELETE("roles/{id}")
    suspend fun deleteRole(@Path("id") id: String): Response<GrowfoneStatusBoolEnvelope<Any?>>

    @POST("roles/assign")
    suspend fun assignRoleBulk(@Body req: AssignRoleBulkRequest): Response<GrowfoneStatusBoolEnvelope<Any?>>

    @POST("roles/remove")
    suspend fun removeRoleBulk(@Body req: RemoveRoleBulkRequest): Response<GrowfoneStatusBoolEnvelope<Any?>>

    @PUT("roles/user/update")
    suspend fun updateUserRole(@Body req: UpdateUserRoleRequest): Response<GrowfoneStatusBoolEnvelope<UserRoleDetailDto>>

    @GET("roles/{roleId}/users")
    suspend fun getUsersByRole(@Path("roleId") roleId: String): Response<GrowfoneStatusBoolEnvelope<List<GrowfoneUserDto>>>

    @GET("users/{userId}/role")
    suspend fun getUserRoleAndPermissions(@Path("userId") userId: String): Response<GrowfoneStatusBoolEnvelope<UserRoleDetailDto>>

    // =========================================================================
    // 9. FEEDBACK (10 · Feedback)
    // =========================================================================

    @GET("getUserFeedback")
    suspend fun getUserFeedback(): Response<GrowfoneStatusBoolEnvelope<List<FeedbackDto>>>

    @POST("createFeedback")
    suspend fun createFeedback(@Body req: CreateFeedbackRequest): Response<GrowfoneStatusBoolEnvelope<FeedbackDto>>

    @POST("updateFeedback")
    suspend fun updateFeedback(@Body req: UpdateFeedbackRequest): Response<GrowfoneStatusBoolEnvelope<FeedbackDto>>

    @DELETE("deleteFeedback/{id}")
    suspend fun deleteFeedback(@Path("id") id: String): Response<GrowfoneStatusBoolEnvelope<Any?>>

    // =========================================================================
    // 10. PLANS & BILLING (11 · Plans & 12 · Billing & Payments)
    // =========================================================================

    @GET("getAllPlans")
    suspend fun getAllPlans(): Response<GrowfoneStatusBoolEnvelope<AllPlansDataDto>>

    @GET("getTeamSubscriptions")
    suspend fun getTeamSubscriptions(): Response<GrowfoneStatusBoolEnvelope<TeamSubscriptionsDataDto>>

    @POST("generatePaymentLink")
    suspend fun generatePaymentLink(@Body req: GeneratePaymentLinkRequest): Response<GrowfoneStatusBoolEnvelope<String>>

    @POST("checkPaymentStatus")
    suspend fun checkPaymentStatus(@Body req: CheckPaymentStatusRequest): Response<GrowfoneStatusBoolEnvelope<List<Any>>>

    @GET("getAllInvoices")
    suspend fun getAllInvoices(@Query("limit") limit: Int? = 10): Response<GrowfoneStatusBoolEnvelope<InvoicesDataDto>>
}
