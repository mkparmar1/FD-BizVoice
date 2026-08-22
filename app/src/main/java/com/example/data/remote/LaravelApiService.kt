package com.example.data.remote

import com.example.data.model.CallRecord
import com.example.data.model.Contact
import com.example.data.model.ContactCreateUpdateRequest
import com.example.data.model.DeviceRegistrationRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.PhoneNumberResponse
import com.example.data.model.SimpleApiResponse
import com.example.data.model.TwilioTokenResponse
import com.example.data.model.UpdateProfileRequest
import com.example.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LaravelApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<SimpleApiResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<SimpleApiResponse>

    @GET("me")
    suspend fun getMe(): Response<User>

    @PUT("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @GET("phone-number")
    suspend fun getPhoneNumber(): Response<PhoneNumberResponse>

    @GET("calls")
    suspend fun getCalls(): Response<List<CallRecord>>

    @GET("calls/{id}")
    suspend fun getCallById(@Path("id") id: String): Response<CallRecord>

    @POST("calls")
    suspend fun recordCall(@Body call: CallRecord): Response<CallRecord>

    @GET("contacts")
    suspend fun getContacts(): Response<List<Contact>>

    @POST("contacts")
    suspend fun createContact(@Body request: ContactCreateUpdateRequest): Response<Contact>

    @PUT("contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body request: ContactCreateUpdateRequest
    ): Response<Contact>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): Response<SimpleApiResponse>

    @POST("twilio/token")
    suspend fun getTwilioToken(): Response<TwilioTokenResponse>

    @POST("device/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<SimpleApiResponse>

    @POST("device/unregister")
    suspend fun unregisterDevice(@Body request: DeviceRegistrationRequest): Response<SimpleApiResponse>
}
