package com.example.data.remote

import com.example.data.model.ApiBaseResponse
import com.example.data.model.ApiResponse
import com.example.data.model.CallDetailDataDto
import com.example.data.model.CallsDataDto
import com.example.data.model.ContactCreateUpdateRequest
import com.example.data.model.ContactDetailDataDto
import com.example.data.model.ContactsDataDto
import com.example.data.model.DeviceDataDto
import com.example.data.model.DeviceRegistrationRequest
import com.example.data.model.DeviceUnregisterRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginDataDto
import com.example.data.model.LoginRequest
import com.example.data.model.MeDataDto
import com.example.data.model.PhoneNumberDataDto
import com.example.data.model.TwilioTokenDataDto
import com.example.data.model.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LaravelApiService {

    // --- AUTH ---
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginDataDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiBaseResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiBaseResponse>

    @GET("me")
    suspend fun getMe(): Response<ApiResponse<MeDataDto>>

    @PUT("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<MeDataDto>>

    // --- PHONE NUMBER ---
    @GET("phone-number")
    suspend fun getPhoneNumber(): Response<ApiResponse<PhoneNumberDataDto>>

    // --- CALLS ---
    @GET("calls")
    suspend fun getCalls(
        @Query("page") page: Int? = 1,
        @Query("search") search: String? = null,
        @Query("direction") direction: String? = null,
        @Query("status") status: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<CallsDataDto>>

    @GET("calls/{id}")
    suspend fun getCallById(@Path("id") id: String): Response<ApiResponse<CallDetailDataDto>>

    // --- CONTACTS ---
    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int? = 1,
        @Query("search") search: String? = null
    ): Response<ApiResponse<ContactsDataDto>>

    @POST("contacts")
    suspend fun createContact(@Body request: ContactCreateUpdateRequest): Response<ApiResponse<ContactDetailDataDto>>

    @GET("contacts/{id}")
    suspend fun getContactById(@Path("id") id: String): Response<ApiResponse<ContactDetailDataDto>>

    @PUT("contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body request: ContactCreateUpdateRequest
    ): Response<ApiResponse<ContactDetailDataDto>>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): Response<ApiBaseResponse>

    // --- TWILIO ---
    @POST("twilio/token")
    suspend fun getTwilioToken(): Response<ApiResponse<TwilioTokenDataDto>>

    // --- DEVICES ---
    @POST("device/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<ApiResponse<DeviceDataDto>>

    @POST("device/unregister")
    suspend fun unregisterDevice(@Body request: DeviceUnregisterRequest): Response<ApiBaseResponse>
}

