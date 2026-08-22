package com.example.data.remote

import com.example.data.model.CallApiDto
import com.example.data.model.CallDetailDataDto
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.model.CallsDataDto
import com.example.data.model.Contact
import com.example.data.model.ContactApiDto
import com.example.data.model.ContactCreateUpdateRequest
import com.example.data.model.ContactDetailDataDto
import com.example.data.model.ContactsDataDto
import com.example.data.model.DeviceDataDto
import com.example.data.model.DeviceDetailDto
import com.example.data.model.DeviceRegistrationRequest
import com.example.data.model.DeviceUnregisterRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginDataDto
import com.example.data.model.LoginRequest
import com.example.data.model.MeDataDto
import com.example.data.model.PaginationDto
import com.example.data.model.PhoneNumberCapabilitiesDto
import com.example.data.model.PhoneNumberDataDto
import com.example.data.model.PhoneNumberDetailDto
import com.example.data.model.PhoneNumberResponse
import com.example.data.model.SimpleApiResponse
import com.example.data.model.TwilioTokenDataDto
import com.example.data.model.TwilioTokenResponse
import com.example.data.model.UpdateProfileRequest
import com.example.data.model.User
import com.example.data.model.UserApiDto
import com.example.data.model.toCallRecord
import com.example.data.model.toContact
import com.example.data.model.toUser
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID

object MockLaravelBackend {
    private var currentUser = User(
        id = "2",
        name = "Alex Mitchell",
        email = "test@example.com",
        assignedPhoneNumber = "+14155551234",
        status = "active",
        company = "BizVoice Global Corp",
        role = "user"
    )

    private val contactsList = mutableListOf(
        Contact(
            id = "5",
            name = "Sarah Jenkins",
            phoneNumber = "+14155550102",
            email = "sarah.j@techflow.com",
            organization = "TechFlow Solutions",
            isDeviceContact = false,
            createdAt = System.currentTimeMillis() - 86400000L * 10
        ),
        Contact(
            id = "6",
            name = "David Chen",
            phoneNumber = "+12125550188",
            email = "d.chen@apexfinancial.com",
            organization = "Apex Financial",
            isDeviceContact = false,
            createdAt = System.currentTimeMillis() - 86400000L * 8
        ),
        Contact(
            id = "7",
            name = "Elena Rostova",
            phoneNumber = "+13125550144",
            email = "elena@vanguardlogistics.com",
            organization = "Vanguard Logistics",
            isDeviceContact = false,
            createdAt = System.currentTimeMillis() - 86400000L * 5
        ),
        Contact(
            id = "8",
            name = "Marcus Brody",
            phoneNumber = "+16505550199",
            email = "mbrody@brodyconsulting.com",
            organization = "Brody Consulting",
            isDeviceContact = false,
            createdAt = System.currentTimeMillis() - 86400000L * 3
        ),
        Contact(
            id = "9",
            name = "Support Desk",
            phoneNumber = "+18005550100",
            email = "support@bizvoice.io",
            organization = "BizVoice Support",
            isDeviceContact = false,
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    private val callsList = mutableListOf(
        CallRecord(
            id = "12",
            remotePhoneNumber = "+14155550000",
            remoteName = "Sarah Jenkins",
            direction = CallDirection.INCOMING,
            durationSeconds = 42,
            status = CallRecordStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
            twilioCallSid = "CA9f2b8c1d4e6a7b3c5d8e9f0a1b2c3d4e",
            notes = "Rate: -0.0085 USD"
        ),
        CallRecord(
            id = "11",
            remotePhoneNumber = "+14155559999",
            remoteName = "David Chen",
            direction = CallDirection.OUTGOING,
            durationSeconds = 0,
            status = CallRecordStatus.NO_ANSWER,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
            twilioCallSid = "CA1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d"
        ),
        CallRecord(
            id = "10",
            remotePhoneNumber = "+16505550199",
            remoteName = "Marcus Brody",
            direction = CallDirection.MISSED,
            durationSeconds = 0,
            status = CallRecordStatus.NO_ANSWER,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30)
        ),
        CallRecord(
            id = "9",
            remotePhoneNumber = "+18005550100",
            remoteName = "Support Desk",
            direction = CallDirection.OUTGOING,
            durationSeconds = 88,
            status = CallRecordStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30)
        )
    )

    private val registeredDevices = mutableSetOf<String>()

    suspend fun login(request: LoginRequest): LoginDataDto {
        delay(350)
        if (request.email.isBlank() || request.password.isBlank()) {
            throw Exception("The email and password fields are required.")
        }
        val userToReturn = if (request.email.contains("@")) {
            currentUser.copy(
                email = request.email,
                name = request.email.substringBefore("@").replace(".", " ").capitalizeWords()
            )
        } else {
            currentUser
        }
        currentUser = userToReturn

        return LoginDataDto(
            token = "3|K9mXq2LpR7vT4nW8yZ1cB6dF0gH5jS3aE7iO2uY4",
            user = UserApiDto(
                id = currentUser.id.toLongOrNull() ?: 2L,
                name = currentUser.name,
                email = currentUser.email,
                role = currentUser.role,
                status = currentUser.status,
                emailVerifiedAt = Instant.now().toString(),
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            ),
            assignedNumber = currentUser.assignedPhoneNumber,
            twilioToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.simulated_jwt_token",
            twilioTokenTtl = 3600
        )
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): SimpleApiResponse {
        delay(250)
        return SimpleApiResponse(
            success = true,
            message = "Password reset link sent to ${request.email}"
        )
    }

    suspend fun getMe(): MeDataDto {
        delay(150)
        return MeDataDto(
            user = UserApiDto(
                id = currentUser.id.toLongOrNull() ?: 2L,
                name = currentUser.name,
                email = currentUser.email,
                role = currentUser.role,
                status = currentUser.status,
                emailVerifiedAt = Instant.now().toString(),
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            ),
            assignedNumber = currentUser.assignedPhoneNumber,
            twilioToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.simulated_jwt_token",
            twilioTokenTtl = 3600
        )
    }

    suspend fun updateProfile(request: UpdateProfileRequest): MeDataDto {
        delay(200)
        currentUser = currentUser.copy(
            name = request.name,
            email = request.email,
            assignedPhoneNumber = request.assignedPhoneNumber ?: currentUser.assignedPhoneNumber,
            company = request.company ?: currentUser.company,
            role = request.role ?: currentUser.role,
            status = request.status ?: currentUser.status
        )
        return getMe()
    }

    suspend fun getPhoneNumber(): PhoneNumberDataDto {
        delay(150)
        val num = currentUser.assignedPhoneNumber
        return if (!num.isNullOrBlank()) {
            PhoneNumberDataDto(
                phoneNumber = PhoneNumberDetailDto(
                    id = 1L,
                    phoneNumber = num,
                    friendlyName = "(415) 555-1234",
                    countryCode = "US",
                    isoCountry = "US",
                    capabilities = PhoneNumberCapabilitiesDto(voice = true, sms = true, mms = false),
                    status = "assigned",
                    purchasedAt = Instant.now().toString(),
                    assignedAt = Instant.now().toString()
                )
            )
        } else {
            PhoneNumberDataDto(phoneNumber = null)
        }
    }

    suspend fun getCalls(page: Int = 1, search: String? = null): CallsDataDto {
        delay(200)
        val filteredCalls = if (!search.isNullOrBlank()) {
            val cleanQuery = search.trim()
            callsList.filter { call ->
                call.remotePhoneNumber.contains(cleanQuery) ||
                (call.remoteName != null && call.remoteName.contains(cleanQuery, ignoreCase = true))
            }
        } else {
            callsList
        }
        val dtos = filteredCalls.sortedByDescending { it.timestamp }.map { call ->
            val statusStr = when (call.status) {
                CallRecordStatus.COMPLETED -> "completed"
                CallRecordStatus.BUSY -> "busy"
                CallRecordStatus.NO_ANSWER -> "no-answer"
                CallRecordStatus.FAILED -> "failed"
                CallRecordStatus.CANCELED -> "canceled"
            }
            CallApiDto(
                id = call.id.toLongOrNull() ?: 12L,
                twilioCallSid = call.twilioCallSid,
                direction = if (call.direction == CallDirection.INCOMING || call.direction == CallDirection.MISSED) "incoming" else "outgoing",
                fromNumber = if (call.direction == CallDirection.INCOMING || call.direction == CallDirection.MISSED) call.remotePhoneNumber else currentUser.assignedPhoneNumber ?: "+14155551234",
                toNumber = if (call.direction == CallDirection.OUTGOING) call.remotePhoneNumber else currentUser.assignedPhoneNumber ?: "+14155551234",
                status = statusStr,
                duration = call.durationSeconds,
                price = "-0.0085",
                currency = "USD",
                startedAt = Instant.ofEpochMilli(call.timestamp).toString(),
                answeredAt = Instant.ofEpochMilli(call.timestamp + 5000).toString(),
                endedAt = Instant.ofEpochMilli(call.timestamp + call.durationSeconds * 1000).toString()
            )
        }
        return CallsDataDto(
            calls = dtos,
            pagination = PaginationDto(currentPage = page, lastPage = 1, perPage = 20, total = dtos.size)
        )
    }

    suspend fun getCallById(id: String): CallDetailDataDto {
        delay(150)
        val call = callsList.firstOrNull { it.id == id }
            ?: throw Exception("No query results for model [App\\Models\\Call] $id.")
        val statusStr = when (call.status) {
            CallRecordStatus.COMPLETED -> "completed"
            CallRecordStatus.BUSY -> "busy"
            CallRecordStatus.NO_ANSWER -> "no-answer"
            CallRecordStatus.FAILED -> "failed"
            CallRecordStatus.CANCELED -> "canceled"
        }
        return CallDetailDataDto(
            call = CallApiDto(
                id = call.id.toLongOrNull() ?: 12L,
                twilioCallSid = call.twilioCallSid,
                direction = if (call.direction == CallDirection.INCOMING || call.direction == CallDirection.MISSED) "incoming" else "outgoing",
                fromNumber = if (call.direction == CallDirection.INCOMING || call.direction == CallDirection.MISSED) call.remotePhoneNumber else currentUser.assignedPhoneNumber ?: "+14155551234",
                toNumber = if (call.direction == CallDirection.OUTGOING) call.remotePhoneNumber else currentUser.assignedPhoneNumber ?: "+14155551234",
                status = statusStr,
                duration = call.durationSeconds,
                price = "-0.0085",
                currency = "USD",
                startedAt = Instant.ofEpochMilli(call.timestamp).toString(),
                phoneNumber = PhoneNumberDetailDto(
                    id = 1L,
                    phoneNumber = currentUser.assignedPhoneNumber ?: "+14155551234"
                )
            )
        )
    }

    suspend fun recordCall(call: CallRecord): CallRecord {
        delay(100)
        callsList.add(0, call)
        return call
    }

    suspend fun getContacts(page: Int = 1, search: String? = null): ContactsDataDto {
        delay(180)
        val filtered = if (!search.isNullOrBlank()) {
            contactsList.filter {
                it.name.contains(search, ignoreCase = true) || it.phoneNumber.contains(search)
            }
        } else {
            contactsList
        }.sortedBy { it.name }

        val dtos = filtered.map {
            ContactApiDto(
                id = it.id.toLongOrNull() ?: 5L,
                name = it.name,
                phoneNumber = it.phoneNumber,
                email = it.email,
                createdAt = Instant.ofEpochMilli(it.createdAt).toString(),
                updatedAt = Instant.ofEpochMilli(it.createdAt).toString()
            )
        }

        return ContactsDataDto(
            contacts = dtos,
            pagination = PaginationDto(currentPage = page, lastPage = 1, perPage = 20, total = dtos.size)
        )
    }

    suspend fun createContact(request: ContactCreateUpdateRequest): ContactDetailDataDto {
        delay(250)
        if (request.name.isBlank()) throw Exception("The name field is required.")
        if (request.phoneNumber.isBlank()) throw Exception("The phone number field is required.")

        val newId = (System.currentTimeMillis() % 100000 + 10)
        val newContact = Contact(
            id = newId.toString(),
            name = request.name,
            phoneNumber = request.phoneNumber,
            email = request.email,
            organization = request.organization,
            isDeviceContact = false,
            createdAt = System.currentTimeMillis()
        )
        contactsList.add(0, newContact)
        return ContactDetailDataDto(
            contact = ContactApiDto(
                id = newId,
                name = newContact.name,
                phoneNumber = newContact.phoneNumber,
                email = newContact.email,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        )
    }

    suspend fun getContactById(id: String): ContactDetailDataDto {
        delay(100)
        val item = contactsList.firstOrNull { it.id == id }
            ?: throw Exception("No query results for model [App\\Models\\Contact] $id.")
        return ContactDetailDataDto(
            contact = ContactApiDto(
                id = item.id.toLongOrNull() ?: 5L,
                name = item.name,
                phoneNumber = item.phoneNumber,
                email = item.email,
                createdAt = Instant.ofEpochMilli(item.createdAt).toString(),
                updatedAt = Instant.ofEpochMilli(item.createdAt).toString()
            )
        )
    }

    suspend fun updateContact(id: String, request: ContactCreateUpdateRequest): ContactDetailDataDto {
        delay(200)
        val index = contactsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = contactsList[index]
            val updated = existing.copy(
                name = request.name.ifBlank { existing.name },
                phoneNumber = request.phoneNumber.ifBlank { existing.phoneNumber },
                email = request.email ?: existing.email,
                organization = request.organization ?: existing.organization
            )
            contactsList[index] = updated
            return ContactDetailDataDto(
                contact = ContactApiDto(
                    id = updated.id.toLongOrNull() ?: 5L,
                    name = updated.name,
                    phoneNumber = updated.phoneNumber,
                    email = updated.email,
                    createdAt = Instant.ofEpochMilli(updated.createdAt).toString(),
                    updatedAt = Instant.now().toString()
                )
            )
        } else {
            throw Exception("No query results for model [App\\Models\\Contact] $id.")
        }
    }

    suspend fun deleteContact(id: String): SimpleApiResponse {
        delay(150)
        contactsList.removeAll { it.id == id }
        return SimpleApiResponse(success = true, message = "Contact deleted")
    }

    suspend fun getTwilioToken(): TwilioTokenDataDto {
        delay(200)
        val num = currentUser.assignedPhoneNumber ?: throw Exception("No phone number assigned")
        return TwilioTokenDataDto(
            token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.simulated_voice_token_bizvoice",
            ttl = 3600,
            identity = "user_${currentUser.id}",
            assignedNumber = num
        )
    }

    suspend fun registerDevice(request: DeviceRegistrationRequest): DeviceDataDto {
        delay(150)
        registeredDevices.add(request.deviceId)
        return DeviceDataDto(
            device = DeviceDetailDto(
                id = 1L,
                userId = currentUser.id.toLongOrNull() ?: 2L,
                deviceId = request.deviceId,
                platform = request.platform,
                pushToken = request.pushToken,
                appVersion = request.appVersion,
                lastSeenAt = Instant.now().toString(),
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        )
    }

    suspend fun unregisterDevice(request: DeviceUnregisterRequest): SimpleApiResponse {
        delay(150)
        registeredDevices.remove(request.deviceId)
        return SimpleApiResponse(success = true, message = "Device unregistered")
    }

    fun toggleSimulatedNumberAssignment(assigned: Boolean) {
        currentUser = if (assigned) {
            currentUser.copy(assignedPhoneNumber = "+14155551234")
        } else {
            currentUser.copy(assignedPhoneNumber = null)
        }
    }

    fun toggleSimulatedUserAccountStatus(active: Boolean) {
        currentUser = currentUser.copy(status = if (active) "active" else "inactive")
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

