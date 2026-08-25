package com.example.data.repository

import android.content.Context
import com.example.data.local.BizVoiceDatabase
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.remote.ApiClient
import com.example.telephony.PhoneNumberFormatter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production Repository for BizVoice.
 * 
 * Directly interfaces with the live Laravel REST backend via ApiClient and
 * persists call logs, contacts, and tokens to local Room SQLite storage.
 */
class BizVoiceRepository(
    private val context: Context,
    val sessionManager: SessionManager,
    private val database: BizVoiceDatabase,
    private val apiClient: ApiClient
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val callRecordDao = database.callRecordDao()
    private val contactDao = database.contactDao()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val countryListType = Types.newParameterizedType(List::class.java, DialingCountryDto::class.java)
    private val countryListAdapter: JsonAdapter<List<DialingCountryDto>> = moshi.adapter(countryListType)

    val currentUserFlow = sessionManager.currentUserFlow
    val authStateFlow = sessionManager.authStateFlow

    // Local cached calls hot StateFlow with duplicate filtering
    val allCallsFlow: StateFlow<List<CallRecord>> = callRecordDao.getAllCalls()
        .map { list ->
            val records = list.map { it.toCallRecord() }
            deduplicateCallRecords(records)
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _dialingCountriesFlow = MutableStateFlow<List<DialingCountry>>(loadInitialCountries())
    val dialingCountriesFlow: StateFlow<List<DialingCountry>> = _dialingCountriesFlow.asStateFlow()

    private val _selectedDialerCountryFlow = MutableStateFlow<DialingCountry>(resolveInitialCountry(_dialingCountriesFlow.value))
    val selectedDialerCountryFlow: StateFlow<DialingCountry> = _selectedDialerCountryFlow.asStateFlow()

    private fun deduplicateCallRecords(records: List<CallRecord>): List<CallRecord> {
        val result = mutableListOf<CallRecord>()
        for (rec in records) {
            val isDuplicate = result.any { existing ->
                existing.id == rec.id ||
                (existing.remotePhoneNumber == rec.remotePhoneNumber &&
                 existing.direction == rec.direction &&
                 kotlin.math.abs(existing.timestamp - rec.timestamp) < 5000L &&
                 existing.durationSeconds == rec.durationSeconds)
            }
            if (!isDuplicate) {
                result.add(rec)
            }
        }
        return result
    }

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
            refreshDialingCountries()
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
            val response = apiClient.getService().adminLogin(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val data = body.data ?: return@withContext Result.failure(Exception(body.message ?: "Invalid credentials"))
                val user = data.user.toUser()
                val token = data.token

                sessionManager.saveAuth(token = token, user = user, deviceAuthKey = user.authKey)
                refreshUserData()
                refreshCalls()
                refreshContacts()
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed. Code: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
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
            val res = apiClient.getService().createProfile(
                CreateProfileRequest(email = email, password = password, name = name, phoneNumber = phoneNumber)
            )
            if (res.isSuccessful && res.body()?.data != null) {
                val user = res.body()!!.data!!.toUser()
                Result.success(user)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().forgotPassword(ForgotPasswordRequest(email))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                Result.success(SimpleApiResponse(success = body.status ?: true, message = body.message ?: "Reset link sent"))
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to send reset link"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(token: String, password: String): Result<SimpleApiResponse> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().resetPassword(
                ResetPasswordRequest(token = token, password = password, passwordConfirmation = password)
            )
            if (res.isSuccessful && res.body() != null) {
                Result.success(SimpleApiResponse(success = res.body()?.status ?: true, message = res.body()?.message ?: "Password reset"))
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                apiClient.getService().logout()
            } catch (_: Exception) {}
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
            val res = apiClient.getService().getProfile()
            if (res.isSuccessful && res.body()?.data != null) {
                val user = res.body()!!.data!!.toUser()
                sessionManager.updateFullProfile(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch user profile"))
            }
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
            val res = apiClient.getService().updateProfile(req)
            if (res.isSuccessful && res.body()?.data != null) {
                val base = res.body()!!.data!!.toUser()
                val updatedUser = base.copy(
                    email = email ?: base.email,
                    company = company ?: base.company,
                    role = role ?: base.role,
                    status = status ?: base.status
                )
                sessionManager.updateFullProfile(updatedUser)
                Result.success(updatedUser)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update profile"))
            }
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
            val res = apiClient.getService().changePassword(req)
            if (res.isSuccessful && res.body() != null) {
                Result.success(SimpleApiResponse(success = true, message = res.body()?.message ?: "Password changed successfully"))
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Password change failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCredits(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().getCredit()
            val credits = res.body()?.data?.credits ?: 0
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
            val res = apiClient.getService().getNumberDetails(sessionManager.getDeviceAuthKey())
            val phoneDto = res.body()?.data
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
            val res = apiClient.getService().getAreaList(AreaListRequest(country, area))
            val list = res.body()?.data ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findAvailablePhoneNumbers(countryCode: String = "US", areaCode: String = ""): Result<List<AvailableNumberDto>> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().findPhoneNumber(
                secretKey = sessionManager.getSecretKey(),
                req = FindPhoneNumberRequest(countryCode, areaCode)
            )
            val list = res.body()?.data ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun holdPhoneNumber(req: HoldNumberRequest): Result<HoldNumberDataDto> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().holdNumber(
                secretKey = sessionManager.getSecretKey(),
                req = req
            )
            val data = res.body()?.data ?: return@withContext Result.failure(Exception("Failed to reserve number"))
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchasePhoneNumber(phoneNumber: String, friendlyName: String): Result<PurchasedNumberDto> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().purchaseNumbers(
                PurchaseNumberRequest(phoneNumber = phoneNumber, friendlyName = friendlyName, userId = sessionManager.getCurrentUser()?.id)
            )
            val data = res.body()?.data ?: return@withContext Result.failure(Exception("Failed to purchase number"))
            sessionManager.updateAssignedNumber(data.phoneNumber)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCapabilityToken(forceRefresh: Boolean = false): Result<CapabilityTokenDto> = withContext(Dispatchers.IO) {
        try {
            val authKey = sessionManager.getDeviceAuthKey()
            val res = apiClient.getService().getCapabilityToken(authKey)
            if (res.isSuccessful && res.body()?.data != null && !res.body()!!.data!!.token.isNullOrBlank()) {
                val data = res.body()!!.data!!
                sessionManager.saveTwilioVoiceToken(data.token)
                Result.success(data)
            } else {
                val errorBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception("Failed to fetch Capability Token: $errorBody"))
            }
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
            val currentNum = sessionManager.getCurrentUser()?.assignedPhoneNumber ?: "+15416342748"
            val res = apiClient.getService().getCallLogs(
                authToken = sessionManager.getDeviceAuthKey(),
                req = GetCallLogsRequest(twilioNumber = currentNum)
            )
            val calls: List<CallRecord> = if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!.data ?: emptyList()
                list.map { it.toCallRecord() }
            } else {
                allCallsFlow.value
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
            val res = apiClient.getService().getContacts(
                name = search,
                isDnd = isDnd,
                isBlacklisted = isBlacklisted
            )
            val contacts: List<Contact> = if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!.data
                data?.data?.map { it.toContact() } ?: emptyList()
            } else {
                allContactsFlow.value
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
            val res = apiClient.getService().createContact(req)
            if (res.isSuccessful && res.body()?.data != null) {
                val created = res.body()!!.data!!.toContact()
                contactDao.insertContact(ContactEntity.fromContact(created))
                Result.success(created)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to save contact"))
            }
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
            val res = apiClient.getService().updateContact(id, req)
            if (res.isSuccessful && res.body()?.data != null) {
                val updated = res.body()!!.data!!.toContact()
                contactDao.insertContact(ContactEntity.fromContact(updated))
                Result.success(updated)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update contact"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContact(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val numericId = id.toLongOrNull() ?: 0L
            val res = apiClient.getService().deleteContact(numericId)
            if (!res.isSuccessful) {
                return@withContext Result.failure(Exception(res.errorBody()?.string() ?: "Failed to delete contact"))
            }
            contactDao.deleteContact(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleContactDnd(id: Long): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().toggleContactDnd(ToggleContactFlagRequest(id))
            if (res.isSuccessful && res.body()?.data != null) {
                val contact = res.body()!!.data!!.toContact()
                contactDao.insertContact(ContactEntity.fromContact(contact))
                Result.success(contact)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to toggle DND"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleContactBlacklist(id: Long): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().toggleContactBlacklist(ToggleContactFlagRequest(id))
            if (res.isSuccessful && res.body()?.data != null) {
                val contact = res.body()!!.data!!.toContact()
                contactDao.insertContact(ContactEntity.fromContact(contact))
                Result.success(contact)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to toggle blacklist"))
            }
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
            val res = apiClient.getService().getDashboard()
            val data = res.body()?.data ?: DashboardStatsDto()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFeedback(): Result<List<FeedbackDto>> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().getUserFeedback()
            val list = res.body()?.data ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFeedback(title: String, description: String, type: String, priority: String): Result<FeedbackDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreateFeedbackRequest(title, description, type, priority)
            val res = apiClient.getService().createFeedback(req)
            val data = res.body()?.data ?: return@withContext Result.failure(Exception("Failed to submit feedback"))
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPlans(): Result<AllPlansDataDto> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().getAllPlans()
            val data = res.body()?.data ?: AllPlansDataDto()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvoices(): Result<InvoicesDataDto> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().getAllInvoices()
            val data = res.body()?.data ?: InvoicesDataDto()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCompany(): Result<CompanyDto> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.getService().getCompany()
            val data = res.body()?.data ?: CompanyDto()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 11. DIALING COUNTRIES & DIALER PREFERENCES
    // ==========================================

    private fun sortCountries(countries: List<DialingCountry>): List<DialingCountry> {
        val enabled = countries.filter { it.enabled }.sortedBy { it.name.lowercase() }
        val disabled = countries.filter { !it.enabled }.sortedBy { it.name.lowercase() }
        return enabled + disabled
    }

    private fun getFallbackCountries(): List<DialingCountry> {
        return try {
            val phoneUtil = PhoneNumberFormatter.getPhoneUtil(context)
            val regions = phoneUtil.supportedRegions
            val list = regions.mapNotNull { region ->
                val countryCode = phoneUtil.getCountryCodeForRegion(region)
                if (countryCode <= 0) return@mapNotNull null
                val locale = java.util.Locale("", region)
                val name = locale.displayCountry.ifBlank { region }
                DialingCountry(
                    isoCode = region.uppercase(java.util.Locale.ROOT),
                    name = name,
                    callingCode = "+$countryCode",
                    continent = null,
                    enabled = true
                )
            }
            if (list.isNotEmpty()) sortCountries(list) else getHardcodedFallbackCountries()
        } catch (_: Exception) {
            getHardcodedFallbackCountries()
        }
    }

    private fun getHardcodedFallbackCountries(): List<DialingCountry> {
        return listOf(
            DialingCountry("IN", "India", "+91", "Asia", true),
            DialingCountry("US", "United States", "+1", "North America", true),
            DialingCountry("GB", "United Kingdom", "+44", "Europe", true),
            DialingCountry("CA", "Canada", "+1", "North America", true),
            DialingCountry("AU", "Australia", "+61", "Oceania", true),
            DialingCountry("SG", "Singapore", "+65", "Asia", true),
            DialingCountry("AE", "United Arab Emirates", "+971", "Middle East", true),
            DialingCountry("DE", "Germany", "+49", "Europe", true),
            DialingCountry("FR", "France", "+33", "Europe", true)
        )
    }

    private fun loadInitialCountries(): List<DialingCountry> {
        val cachedJson = sessionManager.cachedDialingCountriesJson
        if (!cachedJson.isNullOrBlank()) {
            try {
                val dtos = countryListAdapter.fromJson(cachedJson)
                if (!dtos.isNullOrEmpty()) {
                    return sortCountries(dtos.map { it.toDialingCountry() })
                }
            } catch (_: Exception) {}
        }
        return getFallbackCountries()
    }

    private fun resolveInitialCountry(countryList: List<DialingCountry>): DialingCountry {
        val savedIso = sessionManager.selectedDialerCountryIso
        if (!savedIso.isNullOrBlank()) {
            val found = countryList.firstOrNull { it.isoCode.equals(savedIso, ignoreCase = true) }
            if (found != null) return found
        }

        val inferredIso = PhoneNumberFormatter.inferDefaultRegion(context)
        val inferred = countryList.firstOrNull { it.isoCode.equals(inferredIso, ignoreCase = true) }
        if (inferred != null) return inferred

        val defaultIn = countryList.firstOrNull { it.isoCode.equals("IN", ignoreCase = true) }
        if (defaultIn != null) return defaultIn

        return countryList.firstOrNull { it.enabled } ?: countryList.firstOrNull() ?: DialingCountry("IN", "India", "+91", "Asia", true)
    }

    private fun updateSelectedCountryFromList(newList: List<DialingCountry>) {
        val currentSelected = _selectedDialerCountryFlow.value
        val updated = newList.firstOrNull { it.isoCode.equals(currentSelected.isoCode, ignoreCase = true) }
        if (updated != null) {
            _selectedDialerCountryFlow.value = updated
        } else {
            _selectedDialerCountryFlow.value = resolveInitialCountry(newList)
        }
    }

    suspend fun refreshDialingCountries(force: Boolean = false): Result<List<DialingCountry>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedJson = sessionManager.cachedDialingCountriesJson
        val cachedTs = sessionManager.cachedDialingCountriesTimestamp
        val isCacheFresh = (now - cachedTs) < (24 * 60 * 60 * 1000L)

        if (!force && isCacheFresh && !cachedJson.isNullOrBlank()) {
            try {
                val dtos = countryListAdapter.fromJson(cachedJson)
                if (!dtos.isNullOrEmpty()) {
                    val list = sortCountries(dtos.map { it.toDialingCountry() })
                    _dialingCountriesFlow.value = list
                    updateSelectedCountryFromList(list)
                    return@withContext Result.success(list)
                }
            } catch (_: Exception) {}
        }

        val authKey = sessionManager.getDeviceAuthKey()
        try {
            val response = apiClient.getService().getDialingCountries(authKey)
            if (response.isSuccessful && response.body() != null) {
                val envelope = response.body()!!
                val dtos = envelope.data
                if (envelope.status == 200 && !dtos.isNullOrEmpty()) {
                    val list = sortCountries(dtos.map { it.toDialingCountry() })
                    try {
                        val json = countryListAdapter.toJson(dtos)
                        sessionManager.cachedDialingCountriesJson = json
                        sessionManager.cachedDialingCountriesTimestamp = now
                    } catch (_: Exception) {}
                    _dialingCountriesFlow.value = list
                    updateSelectedCountryFromList(list)
                    return@withContext Result.success(list)
                }
            }
        } catch (_: Exception) {}

        // Fallback: cached list if present, else libphonenumber fallback
        val fallbackList = if (!cachedJson.isNullOrBlank()) {
            try {
                countryListAdapter.fromJson(cachedJson)?.map { it.toDialingCountry() }
            } catch (_: Exception) { null }
        } else null

        val finalList = sortCountries(fallbackList ?: getFallbackCountries())
        _dialingCountriesFlow.value = finalList
        updateSelectedCountryFromList(finalList)
        Result.success(finalList)
    }

    fun setSelectedDialerCountry(country: DialingCountry) {
        sessionManager.selectedDialerCountryIso = country.isoCode
        _selectedDialerCountryFlow.value = country
    }

    fun setSelectedDialerCountryByIso(isoCode: String) {
        val found = _dialingCountriesFlow.value.firstOrNull { it.isoCode.equals(isoCode, ignoreCase = true) }
        if (found != null) {
            setSelectedDialerCountry(found)
        }
    }
}
