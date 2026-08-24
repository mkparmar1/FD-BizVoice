package com.example.data.repository

import android.content.Context
import com.example.data.local.BizVoiceDatabase
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SessionManager
import com.example.data.model.*
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

    // Local cached calls hot StateFlow
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

    // Local cached contacts hot StateFlow
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
                refreshUserData()
            }
        }
    }

    // ==========================================
    // 1. AUTHENTICATION
    // ==========================================

    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user: User
            val token: String

            if (sessionManager.useMockBackend) {
                val res = MockLaravelBackend.adminLogin(LoginRequest(email, password))
                user = res.user.toUser()
                token = res.token
            } else {
                val response = apiClient.getService().adminLogin(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val data = body.data ?: return@withContext Result.failure(Exception(body.message ?: "Invalid credentials"))
                    user = data.user.toUser()
                    token = data.token
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed. Code: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMsg))
                }
            }

            sessionManager.saveAuth(token = token, user = user, deviceAuthKey = user.authKey)
            refreshUserData()
            refreshCalls()
            refreshContacts()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProfile(
        email: String,
        password: String,
        name: String?,
        phoneNumber: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user: User
            if (sessionManager.useMockBackend) {
                val dto = MockLaravelBackend.createProfile(
                    CreateProfileRequest(email = email, password = password, name = name, phoneNumber = phoneNumber)
                )
                user = dto.toUser()
            } else {
                val res = apiClient.getService().createProfile(
                    CreateProfileRequest(email = email, password = password, name = name, phoneNumber = phoneNumber)
                )
                if (res.isSuccessful && res.body()?.data != null) {
                    user = res.body()!!.data!!.toUser()
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Registration failed"))
                }
            }
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
                    Result.success(SimpleApiResponse(success = body.status ?: true, message = body.message ?: "Reset link sent"))
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Failed to send reset link"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(token: String, password: String): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            if (sessionManager.useMockBackend) {
                val res = MockLaravelBackend.resetPassword(
                    ResetPasswordRequest(token = token, password = password, passwordConfirmation = password)
                )
                Result.success(res)
            } else {
                val res = apiClient.getService().resetPassword(
                    ResetPasswordRequest(token = token, password = password, passwordConfirmation = password)
                )
                if (res.isSuccessful && res.body() != null) {
                    Result.success(SimpleApiResponse(success = res.body()?.status ?: true, message = res.body()?.message ?: "Password reset"))
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Reset failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.useMockBackend) {
                try {
                    apiClient.getService().logout()
                } catch (_: Exception) {}
            }
            sessionManager.clearSession()
            database.clearAllTables()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    // ==========================================
    // 2. PROFILE & CREDITS
    // ==========================================

    suspend fun refreshUserData(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user: User
            if (sessionManager.useMockBackend) {
                val dto = MockLaravelBackend.getProfile()
                user = dto.toUser()
            } else {
                val res = apiClient.getService().getProfile()
                if (res.isSuccessful && res.body()?.data != null) {
                    user = res.body()!!.data!!.toUser()
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
        email: String? = null,
        assignedPhoneNumber: String? = null,
        company: String? = null,
        role: String? = null,
        status: String? = null,
        phoneNumber: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val effectivePhone = phoneNumber ?: assignedPhoneNumber
            val req = UpdateProfileRequest(name = name, phoneNumber = effectivePhone)
            val updatedUser: User
            if (sessionManager.useMockBackend) {
                val dto = MockLaravelBackend.updateProfile(req)
                val base = dto.toUser()
                updatedUser = base.copy(
                    email = email ?: base.email,
                    company = company ?: base.company,
                    role = role ?: base.role,
                    status = status ?: base.status
                )
            } else {
                val res = apiClient.getService().updateProfile(req)
                if (res.isSuccessful && res.body()?.data != null) {
                    val base = res.body()!!.data!!.toUser()
                    updatedUser = base.copy(
                        email = email ?: base.email,
                        company = company ?: base.company,
                        role = role ?: base.role,
                        status = status ?: base.status
                    )
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

    suspend fun changePassword(
        currentPass: String?,
        newPass: String,
        newPassConfirm: String
    ): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val req = ChangePasswordRequest(
                currentPassword = currentPass,
                newPassword = newPass,
                newPasswordConfirmation = newPassConfirm
            )
            if (sessionManager.useMockBackend) {
                val res = MockLaravelBackend.changePassword(req)
                Result.success(res)
            } else {
                val res = apiClient.getService().changePassword(req)
                if (res.isSuccessful && res.body() != null) {
                    Result.success(SimpleApiResponse(success = true, message = res.body()?.message ?: "Password changed successfully"))
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Password change failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCredits(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val credits: Int
            if (sessionManager.useMockBackend) {
                credits = MockLaravelBackend.getCredit().credits
            } else {
                val res = apiClient.getService().getCredit()
                credits = res.body()?.data?.credits ?: 0
            }
            sessionManager.updateCredits(credits)
            Result.success(credits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 3. PHONE NUMBERS & TELEPHONY
    // ==========================================

    suspend fun fetchAssignedNumber(): Result<PurchasedNumberDto?> = withContext(Dispatchers.IO) {
        try {
            val phoneDto: PurchasedNumberDto?
            if (sessionManager.useMockBackend) {
                phoneDto = MockLaravelBackend.getNumberDetails()
            } else {
                val res = apiClient.getService().getNumberDetails(sessionManager.getDeviceAuthKey())
                phoneDto = res.body()?.data
            }
            if (phoneDto != null && phoneDto.phoneNumber.isNotBlank()) {
                sessionManager.updateAssignedNumber(phoneDto.phoneNumber)
            }
            Result.success(phoneDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAssignedPhoneNumber(): Result<PurchasedNumberDto?> = fetchAssignedNumber()

    suspend fun getAreaList(country: String = "US", area: String = ""): Result<List<AreaCodeDto>> = withContext(Dispatchers.IO) {
        try {
            val list = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getAreaList(AreaListRequest(country, area))
            } else {
                val res = apiClient.getService().getAreaList(AreaListRequest(country, area))
                res.body()?.data ?: emptyList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findAvailablePhoneNumbers(countryCode: String = "US", areaCode: String = ""): Result<List<AvailableNumberDto>> = withContext(Dispatchers.IO) {
        try {
            val list = if (sessionManager.useMockBackend) {
                MockLaravelBackend.findPhoneNumber(FindPhoneNumberRequest(countryCode, areaCode))
            } else {
                val res = apiClient.getService().findPhoneNumber(
                    secretKey = sessionManager.getSecretKey(),
                    req = FindPhoneNumberRequest(countryCode, areaCode)
                )
                res.body()?.data ?: emptyList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun holdPhoneNumber(req: HoldNumberRequest): Result<HoldNumberDataDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.holdNumber(req)
            } else {
                val res = apiClient.getService().holdNumber(
                    secretKey = sessionManager.getSecretKey(),
                    req = req
                )
                res.body()?.data ?: return@withContext Result.failure(Exception("Failed to reserve number"))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchasePhoneNumber(phoneNumber: String, friendlyName: String): Result<PurchasedNumberDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.purchaseNumbers(
                    PurchaseNumberRequest(phoneNumber = phoneNumber, friendlyName = friendlyName, userId = sessionManager.getCurrentUser()?.id)
                )
            } else {
                val res = apiClient.getService().purchaseNumbers(
                    PurchaseNumberRequest(phoneNumber = phoneNumber, friendlyName = friendlyName, userId = sessionManager.getCurrentUser()?.id)
                )
                res.body()?.data ?: return@withContext Result.failure(Exception("Failed to purchase number"))
            }
            sessionManager.updateAssignedNumber(data.phoneNumber)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCapabilityToken(forceRefresh: Boolean = false): Result<CapabilityTokenDto> = withContext(Dispatchers.IO) {
        try {
            val user = sessionManager.getCurrentUser()
            val identity = user?.email?.ifBlank { null } ?: user?.name?.ifBlank { null } ?: "agent_${user?.id ?: 1}"

            val data: CapabilityTokenDto
            if (sessionManager.useMockBackend) {
                data = MockLaravelBackend.getCapabilityToken()
            } else {
                val authKey = sessionManager.getDeviceAuthKey()
                val res = apiClient.getService().getCapabilityToken(authKey)
                if (res.isSuccessful && res.body()?.data != null && !res.body()!!.data!!.token.isNullOrBlank()) {
                    data = res.body()!!.data!!
                } else {
                    val errorBody = res.errorBody()?.string() ?: res.message()
                    return@withContext Result.failure(Exception("Failed to fetch Capability Token: $errorBody"))
                }
            }
            sessionManager.saveTwilioVoiceToken(data.token)
            Result.success(data)
        } catch (e: Exception) {
            val cachedToken = sessionManager.getTwilioVoiceToken()
            if (!forceRefresh && !cachedToken.isNullOrBlank()) {
                val identity = sessionManager.getCurrentUser()?.email ?: "agent_user"
                Result.success(CapabilityTokenDto(identity = identity, token = cachedToken))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getTwilioToken(forceRefresh: Boolean = false): Result<CapabilityTokenDto> = getCapabilityToken(forceRefresh)

    // ==========================================
    // 4. CALLS & RECENTS
    // ==========================================

    suspend fun refreshCalls(search: String? = null): Result<List<CallRecord>> = withContext(Dispatchers.IO) {
        try {
            val calls: List<CallRecord>
            val currentNum = sessionManager.getCurrentUser()?.assignedPhoneNumber ?: "+15416342748"

            if (sessionManager.useMockBackend) {
                calls = MockLaravelBackend.getCallLogs(currentNum).mapIndexed { i, it -> it.toCallRecord(i) }
            } else {
                val res = apiClient.getService().getCallLogs(
                    authToken = sessionManager.getDeviceAuthKey(),
                    req = GetCallLogsRequest(twilioNumber = currentNum)
                )
                if (res.isSuccessful && res.body() != null) {
                    val list = res.body()!!.data ?: emptyList()
                    calls = list.mapIndexed { i, it -> it.toCallRecord(i) }
                } else {
                    calls = allCallsFlow.value
                }
            }

            if (calls.isNotEmpty()) {
                val entities = calls.map { CallRecordEntity.fromCallRecord(it) }
                callRecordDao.insertAllCalls(entities)
            }
            Result.success(calls)
        } catch (e: Exception) {
            Result.success(allCallsFlow.value)
        }
    }

    suspend fun recordCall(call: CallRecord): Result<CallRecord> = withContext(Dispatchers.IO) {
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

    suspend fun recordCompletedCall(call: CallRecord): Result<CallRecord> = recordCall(call)

    suspend fun getCallDetail(id: String): CallRecord? = withContext(Dispatchers.IO) {
        callRecordDao.getCallById(id)?.toCallRecord()
    }

    // ==========================================
    // 5. CONTACTS (CRUD + DND + Blacklist)
    // ==========================================

    suspend fun refreshContacts(
        search: String? = null,
        isDnd: Int? = null,
        isBlacklisted: Int? = null
    ): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val contacts: List<Contact>
            if (sessionManager.useMockBackend) {
                contacts = MockLaravelBackend.getContacts(search = search, isDnd = isDnd, isBlacklisted = isBlacklisted)
                    .data.map { it.toContact() }
            } else {
                val res = apiClient.getService().getContacts(
                    name = search,
                    isDnd = isDnd,
                    isBlacklisted = isBlacklisted
                )
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!.data
                    contacts = data?.data?.map { it.toContact() } ?: emptyList()
                } else {
                    contacts = allContactsFlow.value
                }
            }

            if (contacts.isNotEmpty()) {
                val entities = contacts.map { ContactEntity.fromContact(it) }
                contactDao.insertAllContacts(entities)
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.success(allContactsFlow.value)
        }
    }

    suspend fun createContact(
        name: String,
        phoneNumber: String,
        email: String? = null,
        organization: String? = null,
        isDnd: Boolean = false,
        isBlacklisted: Boolean = false,
        notes: String? = null,
        extension: String? = null
    ): Result<Contact> = withContext(Dispatchers.IO) {
        val parts = name.trim().split(" ", limit = 2)
        val firstName = parts.firstOrNull() ?: name
        val lastName = if (parts.size > 1) parts[1] else null
        createContact(
            firstName = firstName,
            lastName = lastName,
            number = phoneNumber,
            extension = extension,
            email = email,
            companyName = organization,
            notes = notes,
            isDnd = isDnd,
            isBlacklisted = isBlacklisted
        )
    }

    suspend fun createContact(
        firstName: String,
        lastName: String? = null,
        number: String,
        extension: String? = null,
        email: String? = null,
        companyName: String? = null,
        notes: String? = null,
        isDnd: Boolean = false,
        isBlacklisted: Boolean = false
    ): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val created: Contact
            val req = CreateContactRequest(
                firstName = firstName,
                lastName = lastName,
                number = number,
                extension = extension,
                email = email,
                companyName = companyName,
                notes = notes,
                isDnd = isDnd,
                isBlacklisted = isBlacklisted
            )
            if (sessionManager.useMockBackend) {
                created = MockLaravelBackend.createContact(req).toContact()
            } else {
                val res = apiClient.getService().createContact(req)
                if (res.isSuccessful && res.body()?.data != null) {
                    created = res.body()!!.data!!.toContact()
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

    suspend fun updateContact(
        id: String,
        name: String,
        phoneNumber: String,
        email: String? = null,
        organization: String? = null,
        isDnd: Boolean = false,
        isBlacklisted: Boolean = false,
        notes: String? = null,
        extension: String? = null
    ): Result<Contact> = withContext(Dispatchers.IO) {
        val numericId = id.toLongOrNull() ?: 0L
        val parts = name.trim().split(" ", limit = 2)
        val firstName = parts.firstOrNull() ?: name
        val lastName = if (parts.size > 1) parts[1] else null
        updateContact(
            id = numericId,
            firstName = firstName,
            lastName = lastName,
            number = phoneNumber,
            extension = extension,
            email = email,
            companyName = organization,
            notes = notes,
            isDnd = isDnd,
            isBlacklisted = isBlacklisted
        )
    }

    suspend fun updateContact(
        id: Long,
        firstName: String,
        lastName: String? = null,
        number: String,
        extension: String? = null,
        email: String? = null,
        companyName: String? = null,
        notes: String? = null,
        isDnd: Boolean = false,
        isBlacklisted: Boolean = false
    ): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val updated: Contact
            val req = CreateContactRequest(
                firstName = firstName,
                lastName = lastName,
                number = number,
                extension = extension,
                email = email,
                companyName = companyName,
                notes = notes,
                isDnd = isDnd,
                isBlacklisted = isBlacklisted
            )
            if (sessionManager.useMockBackend) {
                updated = MockLaravelBackend.updateContact(id, req).toContact()
            } else {
                val res = apiClient.getService().updateContact(id, req)
                if (res.isSuccessful && res.body()?.data != null) {
                    updated = res.body()!!.data!!.toContact()
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
            val numericId = id.toLongOrNull() ?: 0L
            if (sessionManager.useMockBackend) {
                MockLaravelBackend.deleteContact(numericId)
            } else {
                val res = apiClient.getService().deleteContact(numericId)
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to delete contact"))
                }
            }
            contactDao.deleteContact(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleContactDnd(id: Long): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val contact: Contact
            if (sessionManager.useMockBackend) {
                contact = MockLaravelBackend.toggleContactDnd(id).toContact()
            } else {
                val res = apiClient.getService().toggleContactDnd(ToggleContactFlagRequest(id))
                if (res.isSuccessful && res.body()?.data != null) {
                    contact = res.body()!!.data!!.toContact()
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to toggle DND"))
                }
            }
            contactDao.insertContact(ContactEntity.fromContact(contact))
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleContactBlacklist(id: Long): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val contact: Contact
            if (sessionManager.useMockBackend) {
                contact = MockLaravelBackend.toggleContactBlacklist(id).toContact()
            } else {
                val res = apiClient.getService().toggleContactBlacklist(ToggleContactFlagRequest(id))
                if (res.isSuccessful && res.body()?.data != null) {
                    contact = res.body()!!.data!!.toContact()
                } else {
                    return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to toggle blacklist"))
                }
            }
            contactDao.insertContact(ContactEntity.fromContact(contact))
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkContactByPhone(phone: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByPhone(phone)?.toContact()
    }

    suspend fun syncDeviceContacts(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deviceContacts = mutableListOf<Contact>()
            val resolver = context.contentResolver
            val cursor = resolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                    if (number.isNotBlank()) {
                        deviceContacts.add(
                            Contact(
                                id = "device_$id",
                                name = name,
                                phoneNumber = number,
                                isDeviceContact = true,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            if (deviceContacts.isNotEmpty()) {
                val entities = deviceContacts.map { ContactEntity.fromContact(it) }
                contactDao.insertAllContacts(entities)
            }
            Result.success(deviceContacts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncDeviceContacts(contacts: List<Contact>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = contacts.map { ContactEntity.fromContact(it) }
            contactDao.insertAllContacts(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 6. DASHBOARD & FEEDBACK & PLANS
    // ==========================================

    suspend fun getDashboard(): Result<DashboardStatsDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getDashboard()
            } else {
                val res = apiClient.getService().getDashboard()
                res.body()?.data ?: DashboardStatsDto()
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFeedback(): Result<List<FeedbackDto>> = withContext(Dispatchers.IO) {
        try {
            val list = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getUserFeedback()
            } else {
                val res = apiClient.getService().getUserFeedback()
                res.body()?.data ?: emptyList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFeedback(title: String, description: String, type: String, priority: String): Result<FeedbackDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreateFeedbackRequest(title, description, type, priority)
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.createFeedback(req)
            } else {
                val res = apiClient.getService().createFeedback(req)
                res.body()?.data ?: return@withContext Result.failure(Exception("Failed to submit feedback"))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPlans(): Result<AllPlansDataDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getAllPlans()
            } else {
                val res = apiClient.getService().getAllPlans()
                res.body()?.data ?: AllPlansDataDto()
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvoices(): Result<InvoicesDataDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getAllInvoices()
            } else {
                val res = apiClient.getService().getAllInvoices()
                res.body()?.data ?: InvoicesDataDto()
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCompany(): Result<CompanyDto> = withContext(Dispatchers.IO) {
        try {
            val data = if (sessionManager.useMockBackend) {
                MockLaravelBackend.getCompany()
            } else {
                val res = apiClient.getService().getCompany()
                res.body()?.data ?: CompanyDto()
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
