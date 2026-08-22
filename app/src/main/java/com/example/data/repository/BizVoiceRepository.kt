package com.example.data.repository

import android.content.Context
import com.example.data.local.BizVoiceDatabase
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SessionManager
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.model.Contact
import com.example.data.model.ContactCreateUpdateRequest
import com.example.data.model.DeviceRegistrationRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.PhoneNumberResponse
import com.example.data.model.SimpleApiResponse
import com.example.data.model.TwilioTokenResponse
import com.example.data.model.User
import com.example.data.model.toCallRecord
import com.example.data.model.toContact
import com.example.data.model.toUser
import com.example.data.remote.ApiClient
import com.example.data.remote.MockLaravelBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BizVoiceRepository(
    private val context: Context,
    val sessionManager: SessionManager,
    private val database: BizVoiceDatabase,
    private val apiClient: ApiClient
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val callRecordDao = database.callRecordDao()
    private val contactDao = database.contactDao()

    val currentUserFlow = sessionManager.currentUserFlow
    val authStateFlow = sessionManager.authStateFlow

    // Local cached calls hot StateFlow - keeps in-memory state ready across tab switches
    val allCallsFlow: StateFlow<List<CallRecord>> = callRecordDao.getAllCalls()
        .map { list -> list.map { it.toCallRecord() } }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getCallsForPhoneNumber(phoneNumber: String): Flow<List<CallRecord>> {
        val targetDigits = phoneNumber.filter { it.isDigit() }
        return allCallsFlow.map { list ->
            list.filter { call ->
                val callDigits = call.remotePhoneNumber.filter { it.isDigit() }
                call.remotePhoneNumber == phoneNumber ||
                        (targetDigits.isNotEmpty() && (callDigits.endsWith(targetDigits) || targetDigits.endsWith(callDigits)))
            }
        }
    }

    // Local cached contacts hot StateFlow - keeps in-memory state ready across tab switches
    val allContactsFlow: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .map { list -> list.map { it.toContact() } }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        repositoryScope.launch {
            if (sessionManager.isLoggedIn()) {
                refreshCalls()
                refreshContacts()
            }
        }
    }

    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user: User
            val token: String
            val twilioToken: String?

            if (sessionManager.useMockBackend) {
                val res = MockLaravelBackend.login(LoginRequest(email, password))
                user = res.user.toUser(res.assignedNumber)
                token = res.token
                twilioToken = res.twilioToken
            } else {
                val response = apiClient.getService().login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val data = body.data ?: return@withContext Result.failure(Exception(body.message ?: "Invalid credentials"))
                    user = data.user.toUser(data.assignedNumber)
                    token = data.token
                    twilioToken = data.twilioToken
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed. Code: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMsg))
                }
            }

            sessionManager.saveAuth(token, user, twilioToken)
            registerDevice()
            refreshUserData()
            refreshCalls()
            refreshContacts()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            if (sessionManager.useMockBackend) {
                val res = MockLaravelBackend.forgotPassword(ForgotPasswordRequest(email))
                Result.success(res)
            } else {
                val res = apiClient.getService().forgotPassword(ForgotPasswordRequest(email))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    Result.success(SimpleApiResponse(success = body.success, message = body.message ?: "Reset link sent"))
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Failed to send reset link"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            unregisterDevice()
            if (!sessionManager.useMockBackend) {
                try {
                    apiClient.getService().logout()
                } catch (_: Exception) {}
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    suspend fun refreshUserData(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user: User
            if (sessionManager.useMockBackend) {
                val me = MockLaravelBackend.getMe()
                user = me.user.toUser(me.assignedNumber)
                if (me.twilioToken != null) {
                    sessionManager.saveTwilioVoiceToken(me.twilioToken)
                }
            } else {
                val res = apiClient.getService().getMe()
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data ?: return@withContext Result.failure(Exception("Failed to parse user profile"))
                    user = data.user.toUser(data.assignedNumber)
                    if (data.twilioToken != null) {
                        sessionManager.saveTwilioVoiceToken(data.twilioToken)
                    }
                } else {
                    return@withContext Result.failure(Exception("Failed to fetch user profile"))
                }
            }

            sessionManager.updateFullProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        name: String,
        email: String,
        assignedPhoneNumber: String?,
        company: String?,
        role: String?,
        status: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val req = com.example.data.model.UpdateProfileRequest(
                name = name,
                email = email,
                assignedPhoneNumber = assignedPhoneNumber,
                company = company,
                role = role,
                status = status
            )
            val updatedUser: User
            if (sessionManager.useMockBackend) {
                val me = MockLaravelBackend.updateProfile(req)
                updatedUser = me.user.toUser(me.assignedNumber)
            } else {
                val res = apiClient.getService().updateProfile(req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data ?: return@withContext Result.failure(Exception("Failed to update profile"))
                    updatedUser = data.user.toUser(data.assignedNumber)
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update profile"))
                }
            }

            sessionManager.updateFullProfile(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAssignedPhoneNumber(): Result<PhoneNumberResponse> = withContext(Dispatchers.IO) {
        try {
            val phoneRes: PhoneNumberResponse
            if (sessionManager.useMockBackend) {
                val dto = MockLaravelBackend.getPhoneNumber()
                phoneRes = PhoneNumberResponse(
                    phoneNumber = dto.phoneNumber?.phoneNumber,
                    status = dto.phoneNumber?.status ?: "unavailable",
                    callerName = dto.phoneNumber?.friendlyName
                )
            } else {
                val res = apiClient.getService().getPhoneNumber()
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data
                    phoneRes = PhoneNumberResponse(
                        phoneNumber = data?.phoneNumber?.phoneNumber,
                        status = data?.phoneNumber?.status ?: "unavailable",
                        callerName = data?.phoneNumber?.friendlyName
                    )
                } else {
                    return@withContext Result.failure(Exception("Failed to fetch assigned number"))
                }
            }

            sessionManager.updateAssignedNumber(phoneRes.phoneNumber)
            Result.success(phoneRes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshCalls(search: String? = null): Result<List<CallRecord>> = withContext(Dispatchers.IO) {
        try {
            val calls: List<CallRecord>
            if (sessionManager.useMockBackend) {
                calls = MockLaravelBackend.getCalls(search = search).calls.map { it.toCallRecord() }
            } else {
                val res = apiClient.getService().getCalls(search = search)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data
                    calls = data?.calls?.map { it.toCallRecord() } ?: emptyList()
                } else {
                    return@withContext Result.failure(Exception("Failed to fetch call records"))
                }
            }

            val entities = calls.map { CallRecordEntity.fromCallRecord(it) }
            callRecordDao.insertAllCalls(entities)
            Result.success(calls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordCompletedCall(call: CallRecord): Result<CallRecord> = withContext(Dispatchers.IO) {
        try {
            callRecordDao.insertCall(CallRecordEntity.fromCallRecord(call))

            if (sessionManager.useMockBackend) {
                MockLaravelBackend.recordCall(call)
            }
            Result.success(call)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCallDetail(callId: String): CallRecord? = withContext(Dispatchers.IO) {
        val cached = callRecordDao.getCallById(callId)?.toCallRecord()
        if (cached != null) return@withContext cached

        if (sessionManager.useMockBackend) {
            try {
                MockLaravelBackend.getCallById(callId).call.toCallRecord()
            } catch (e: Exception) {
                null
            }
        } else {
            try {
                val res = apiClient.getService().getCallById(callId)
                res.body()?.data?.call?.toCallRecord()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun refreshContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val contacts: List<Contact>
            if (sessionManager.useMockBackend) {
                contacts = MockLaravelBackend.getContacts().contacts.map { it.toContact() }
            } else {
                val res = apiClient.getService().getContacts()
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data
                    contacts = data?.contacts?.map { it.toContact() } ?: emptyList()
                } else {
                    return@withContext Result.failure(Exception("Failed to fetch contacts"))
                }
            }

            val entities = contacts.map { ContactEntity.fromContact(it) }
            contactDao.insertAllContacts(entities)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createContact(name: String, phoneNumber: String, email: String?, organization: String?): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val created: Contact
            val req = ContactCreateUpdateRequest(name, phoneNumber, email, organization)
            if (sessionManager.useMockBackend) {
                created = MockLaravelBackend.createContact(req).contact.toContact()
            } else {
                val res = apiClient.getService().createContact(req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data ?: return@withContext Result.failure(Exception("Failed to save contact"))
                    created = data.contact.toContact()
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to save contact"))
                }
            }

            contactDao.insertContact(ContactEntity.fromContact(created))
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContact(id: String, name: String, phoneNumber: String, email: String?, organization: String?): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val updated: Contact
            val req = ContactCreateUpdateRequest(name, phoneNumber, email, organization)
            if (sessionManager.useMockBackend) {
                updated = MockLaravelBackend.updateContact(id, req).contact.toContact()
            } else {
                val res = apiClient.getService().updateContact(id, req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data ?: return@withContext Result.failure(Exception("Failed to update contact"))
                    updated = data.contact.toContact()
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update contact"))
                }
            }

            contactDao.insertContact(ContactEntity.fromContact(updated))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContact(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (sessionManager.useMockBackend) {
                MockLaravelBackend.deleteContact(id)
            } else {
                val res = apiClient.getService().deleteContact(id)
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to delete contact"))
                }
            }

            contactDao.deleteContact(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTwilioToken(): Result<TwilioTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val token: TwilioTokenResponse
            if (sessionManager.useMockBackend) {
                val data = MockLaravelBackend.getTwilioToken()
                token = TwilioTokenResponse(
                    token = data.token,
                    identity = data.identity,
                    expiresIn = data.ttl ?: 3600
                )
            } else {
                val res = apiClient.getService().getTwilioToken()
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data ?: return@withContext Result.failure(Exception("Failed to parse Twilio Voice token"))
                    token = TwilioTokenResponse(
                        token = data.token,
                        identity = data.identity,
                        expiresIn = data.ttl ?: 3600
                    )
                } else {
                    return@withContext Result.failure(Exception("Failed to obtain Twilio Voice token"))
                }
            }
            sessionManager.saveTwilioVoiceToken(token.token)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun registerDevice() {
        val req = DeviceRegistrationRequest(
            deviceId = "android_device_" + android.os.Build.MODEL.replace(" ", "_"),
            platform = "android",
            pushToken = sessionManager.devicePushToken,
            appVersion = "1.0.0"
        )
        try {
            if (sessionManager.useMockBackend) {
                MockLaravelBackend.registerDevice(req)
            } else {
                apiClient.getService().registerDevice(req)
            }
        } catch (_: Exception) {}
    }

    private suspend fun unregisterDevice() {
        val req = com.example.data.model.DeviceUnregisterRequest(
            deviceId = "android_device_" + android.os.Build.MODEL.replace(" ", "_")
        )
        try {
            if (sessionManager.useMockBackend) {
                MockLaravelBackend.unregisterDevice(req)
            } else {
                apiClient.getService().unregisterDevice(req)
            }
        } catch (_: Exception) {}
    }

    suspend fun syncDeviceContacts(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone._ID,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor = resolver.query(
                uri,
                projection,
                null,
                null,
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val deviceEntities = mutableListOf<ContactEntity>()
            cursor?.use { c ->
                val idCol = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone._ID)
                val nameCol = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (c.moveToNext()) {
                    val rawId = if (idCol != -1) c.getString(idCol) else java.util.UUID.randomUUID().toString()
                    val rawName = if (nameCol != -1) c.getString(nameCol) ?: "Unknown" else "Unknown"
                    val rawNum = if (numCol != -1) c.getString(numCol) ?: "" else ""
                    val photoUri = if (photoCol != -1) c.getString(photoCol) else null

                    val cleanNum = rawNum.trim()
                    val digitKey = cleanNum.filter { it.isDigit() }
                    if (cleanNum.isNotBlank() && (digitKey.isEmpty() || seenNumbers.add(digitKey))) {
                        val contactId = "device_${rawId}_${digitKey.takeLast(6)}"
                        deviceEntities.add(
                            ContactEntity(
                                id = contactId,
                                name = rawName,
                                phoneNumber = cleanNum,
                                email = null,
                                organization = "Mobile Contact",
                                avatarUrl = photoUri,
                                isDeviceContact = true,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            // Replace old synced device contacts with fresh synced list
            contactDao.clearDeviceContacts()
            if (deviceEntities.isNotEmpty()) {
                contactDao.insertAllContacts(deviceEntities)
            }
            Result.success(deviceEntities.size)
        } catch (e: SecurityException) {
            Result.failure(Exception("Contacts permission denied. Please grant permission in Settings."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearDeviceContacts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            contactDao.clearDeviceContacts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkContactByPhone(phone: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByPhone(phone)?.toContact()
    }
}
