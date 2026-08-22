package com.example.data.remote

import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.model.Contact
import com.example.data.model.ContactCreateUpdateRequest
import com.example.data.model.DeviceRegistrationRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.PhoneNumberResponse
import com.example.data.model.SimpleApiResponse
import com.example.data.model.TwilioTokenResponse
import com.example.data.model.User
import kotlinx.coroutines.delay
import java.util.UUID

object MockLaravelBackend {
    private var currentUser = User(
        id = "usr_99824",
        name = "Alex Mitchell",
        email = "alex.mitchell@bizvoice.io",
        assignedPhoneNumber = "+1 (415) 555-0101",
        status = "active",
        company = "Acme Global Solutions",
        role = "Senior Account Executive"
    )

    private val contactsList = mutableListOf(
        Contact(
            id = "cnt_101",
            name = "Sarah Jenkins",
            phoneNumber = "+1 (415) 555-0102",
            email = "sarah.j@techflow.com",
            organization = "TechFlow Solutions",
            isDeviceContact = false
        ),
        Contact(
            id = "cnt_102",
            name = "David Chen",
            phoneNumber = "+1 (212) 555-0188",
            email = "d.chen@apexfinancial.com",
            organization = "Apex Financial",
            isDeviceContact = false
        ),
        Contact(
            id = "cnt_103",
            name = "Elena Rostova",
            phoneNumber = "+1 (312) 555-0144",
            email = "elena@vanguardlogistics.com",
            organization = "Vanguard Logistics",
            isDeviceContact = false
        ),
        Contact(
            id = "cnt_104",
            name = "Marcus Brody",
            phoneNumber = "+1 (650) 555-0199",
            email = "mbrody@brodyconsulting.com",
            organization = "Brody Consulting",
            isDeviceContact = false
        ),
        Contact(
            id = "cnt_105",
            name = "Support Desk",
            phoneNumber = "+1 (800) 555-0100",
            email = "support@bizvoice.io",
            organization = "BizVoice Support",
            isDeviceContact = false
        )
    )

    private val callsList = mutableListOf(
        CallRecord(
            id = "call_901",
            remotePhoneNumber = "+1 (415) 555-0102",
            remoteName = "Sarah Jenkins",
            direction = CallDirection.OUTGOING,
            durationSeconds = 154,
            status = CallRecordStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 25, // 25 mins ago
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30),
            isRecorded = true,
            recordingDurationSeconds = 154,
            recordingUrl = "https://recordings.bizvoice.io/rec_call_901.wav"
        ),
        CallRecord(
            id = "call_902",
            remotePhoneNumber = "+1 (212) 555-0188",
            remoteName = "David Chen",
            direction = CallDirection.INCOMING,
            durationSeconds = 342,
            status = CallRecordStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30),
            isRecorded = true,
            recordingDurationSeconds = 342,
            recordingUrl = "https://recordings.bizvoice.io/rec_call_902.wav"
        ),
        CallRecord(
            id = "call_903",
            remotePhoneNumber = "+1 (650) 555-0199",
            remoteName = "Marcus Brody",
            direction = CallDirection.MISSED,
            durationSeconds = 0,
            status = CallRecordStatus.NO_ANSWER,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5, // 5 hours ago
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30),
            isRecorded = false,
            recordingDurationSeconds = 0
        ),
        CallRecord(
            id = "call_904",
            remotePhoneNumber = "+1 (800) 555-0100",
            remoteName = "Support Desk",
            direction = CallDirection.OUTGOING,
            durationSeconds = 88,
            status = CallRecordStatus.COMPLETED,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26, // Yesterday
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 30),
            isRecorded = false,
            recordingDurationSeconds = 0
        )
    )

    private val registeredDevices = mutableSetOf<String>()

    suspend fun login(request: LoginRequest): LoginResponse {
        delay(400)
        if (request.email.isBlank() || request.password.isBlank()) {
            throw Exception("Invalid credentials. Please enter email and password.")
        }
        val userToReturn = if (request.email.contains("@")) {
            currentUser.copy(email = request.email, name = request.email.substringBefore("@").replace(".", " ").capitalizeWords())
        } else {
            currentUser
        }
        currentUser = userToReturn
        return LoginResponse(
            token = "bv_token_" + UUID.randomUUID().toString().replace("-", ""),
            tokenType = "Bearer",
            user = currentUser
        )
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): SimpleApiResponse {
        delay(350)
        return SimpleApiResponse(
            success = true,
            message = "Password reset instructions sent to ${request.email}"
        )
    }

    suspend fun getMe(): User {
        delay(200)
        return currentUser
    }

    suspend fun updateProfile(request: com.example.data.model.UpdateProfileRequest): User {
        delay(250)
        currentUser = currentUser.copy(
            name = request.name,
            email = request.email,
            assignedPhoneNumber = request.assignedPhoneNumber ?: currentUser.assignedPhoneNumber,
            company = request.company ?: currentUser.company,
            role = request.role ?: currentUser.role,
            status = request.status ?: currentUser.status
        )
        return currentUser
    }

    suspend fun getPhoneNumber(): PhoneNumberResponse {
        delay(200)
        return PhoneNumberResponse(
            phoneNumber = currentUser.assignedPhoneNumber,
            status = if (currentUser.assignedPhoneNumber.isNullOrBlank()) "unavailable" else "active",
            callerName = currentUser.company
        )
    }

    suspend fun getCalls(): List<CallRecord> {
        delay(250)
        return callsList.sortedByDescending { it.timestamp }
    }

    suspend fun getCallById(id: String): CallRecord {
        delay(150)
        return callsList.firstOrNull { it.id == id }
            ?: throw Exception("Call not found with id $id")
    }

    suspend fun recordCall(call: CallRecord): CallRecord {
        delay(150)
        callsList.add(0, call)
        return call
    }

    suspend fun getContacts(): List<Contact> {
        delay(200)
        return contactsList.sortedBy { it.name }
    }

    suspend fun createContact(request: ContactCreateUpdateRequest): Contact {
        delay(300)
        val newContact = Contact(
            id = "cnt_" + UUID.randomUUID().toString().substring(0, 6),
            name = request.name,
            phoneNumber = request.phoneNumber,
            email = request.email,
            organization = request.organization,
            isDeviceContact = false,
            createdAt = System.currentTimeMillis()
        )
        contactsList.add(0, newContact)
        return newContact
    }

    suspend fun updateContact(id: String, request: ContactCreateUpdateRequest): Contact {
        delay(300)
        val index = contactsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = contactsList[index]
            val updated = existing.copy(
                name = request.name,
                phoneNumber = request.phoneNumber,
                email = request.email,
                organization = request.organization
            )
            contactsList[index] = updated
            return updated
        } else {
            throw Exception("Contact not found")
        }
    }

    suspend fun deleteContact(id: String): SimpleApiResponse {
        delay(250)
        val removed = contactsList.removeAll { it.id == id }
        if (removed) {
            return SimpleApiResponse(success = true, message = "Contact deleted successfully")
        } else {
            throw Exception("Contact not found")
        }
    }

    suspend fun getTwilioToken(): TwilioTokenResponse {
        delay(250)
        return TwilioTokenResponse(
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJTS2JpenZvaWNlIiwic3ViIjoidXNyXzk5ODI0IiwiZXhwIjoxNzg0ODIwMDAwfQ.simulated_twilio_signature",
            identity = currentUser.id,
            expiresIn = 3600,
            accountSid = "AC" + UUID.randomUUID().toString().replace("-", "").substring(0, 30)
        )
    }

    suspend fun registerDevice(request: DeviceRegistrationRequest): SimpleApiResponse {
        delay(150)
        registeredDevices.add(request.deviceId)
        return SimpleApiResponse(success = true, message = "Device registered for VoIP push notifications")
    }

    suspend fun unregisterDevice(request: DeviceRegistrationRequest): SimpleApiResponse {
        delay(150)
        registeredDevices.remove(request.deviceId)
        return SimpleApiResponse(success = true, message = "Device unregistered")
    }

    fun toggleSimulatedNumberAssignment(assigned: Boolean) {
        currentUser = if (assigned) {
            currentUser.copy(assignedPhoneNumber = "+1 (415) 555-0101")
        } else {
            currentUser.copy(assignedPhoneNumber = null)
        }
    }

    fun toggleSimulatedUserAccountStatus(active: Boolean) {
        currentUser = currentUser.copy(status = if (active) "active" else "suspended")
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
