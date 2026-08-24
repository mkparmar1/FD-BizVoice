package com.example.data.remote

import com.example.data.model.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID

object MockLaravelBackend {
    private var currentUser = GrowfoneUserDto(
        id = "9b1f2c34-5d6e-4a7b-8c9d-0e1f2a3b4c5d",
        name = "Team Member",
        email = "team@demo.test",
        emailVerifiedAt = "2026-01-14T09:21:33.000000Z",
        phoneNumber = "+12183061691",
        socialId = "",
        credits = 250,
        authKey = "3f9a1c2e-7b4d-4e8f-9a0b-1c2d3e4f5a6b",
        rpayCustomerId = "cust_NxAbCdEfGhIjKl",
        profilePicture = "",
        friendlyName = "(218) 306-1691",
        status = "active",
        enableReminderNotes = true,
        enablePlayRecording = true,
        newLogin = false,
        parentUserId = "",
        roleId = "7a2b3c4d-5e6f-4071-8293-a4b5c6d7e8f9",
        createdAt = "2026-01-14T09:21:33.000000Z",
        updatedAt = "2026-08-24T11:02:17.000000Z"
    )

    private var company = CompanyDto(
        id = "5f6a7b8c-9d0e-4f12-8345-6789abcdef01",
        userId = "9b1f2c34-5d6e-4a7b-8c9d-0e1f2a3b4c5d",
        name = "Growfone Communications Pvt Ltd",
        email = "billing@growfone.com",
        website = "https://growfone.com",
        mobileNumber = "9876543210",
        gstNumber = "24AAACA1234A1Z5",
        address = "401 Iscon Emporio, Satellite",
        city = "Ahmedabad",
        state = "Gujarat",
        country = "India",
        postalCode = "380015",
        industryType = "Telecommunications",
        description = "Cloud telephony reseller",
        status = "active"
    )

    private val contactsList = mutableListOf(
        GrowfoneContactDto(
            id = 42,
            firstName = "Priya",
            lastName = "Sharma",
            fullName = "Priya Sharma",
            number = "+919876543210",
            extension = "204",
            email = "priya.sharma@acme.com",
            companyName = "Acme Corp",
            notes = "Prefers calls after 4pm IST",
            isDnd = false,
            isBlacklisted = false,
            createdAt = "2026-08-22 14:08:11",
            updatedAt = "2026-08-22 14:08:11"
        ),
        GrowfoneContactDto(
            id = 41,
            firstName = "Daniel",
            lastName = "Okafor",
            fullName = "Daniel Okafor",
            number = "+14155550132",
            extension = "",
            email = "daniel@northwind.io",
            companyName = "Northwind Logistics",
            notes = "Executive account manager",
            isDnd = true,
            isBlacklisted = false,
            createdAt = "2026-08-21 09:33:47",
            updatedAt = "2026-08-23 11:02:19"
        ),
        GrowfoneContactDto(
            id = 40,
            firstName = "Sarah",
            lastName = "Jenkins",
            fullName = "Sarah Jenkins",
            number = "+14155550102",
            extension = "102",
            email = "sarah.j@techflow.com",
            companyName = "TechFlow Solutions",
            notes = "Key client for VoIP rollout",
            isDnd = false,
            isBlacklisted = false,
            createdAt = "2026-08-20 10:15:00",
            updatedAt = "2026-08-20 10:15:00"
        ),
        GrowfoneContactDto(
            id = 39,
            firstName = "Marcus",
            lastName = "Brody",
            fullName = "Marcus Brody",
            number = "+16505550199",
            extension = "",
            email = "mbrody@brodyconsulting.com",
            companyName = "Brody Consulting",
            notes = "Requested DND on weekends",
            isDnd = false,
            isBlacklisted = true,
            createdAt = "2026-08-19 16:20:00",
            updatedAt = "2026-08-19 16:20:00"
        )
    )

    private val callLogs = mutableListOf(
        CallLogDto(
            toPhoneNumber = "+919876543210",
            fromPhoneNumber = "+15416342748",
            startTime = "2026-08-24 07:41:55",
            duration = 142,
            direction = "outbound",
            status = "completed"
        ),
        CallLogDto(
            toPhoneNumber = "+15416342748",
            fromPhoneNumber = "+14155550132",
            startTime = "2026-08-23 16:12:04",
            duration = 0,
            direction = "inbound",
            status = "no-answer"
        ),
        CallLogDto(
            toPhoneNumber = "+14155550102",
            fromPhoneNumber = "+15416342748",
            startTime = "2026-08-23 11:30:22",
            duration = 88,
            direction = "outbound",
            status = "completed"
        ),
        CallLogDto(
            toPhoneNumber = "+15416342748",
            fromPhoneNumber = "+16505550199",
            startTime = "2026-08-22 14:05:10",
            duration = 0,
            direction = "inbound",
            status = "busy"
        )
    )

    private val teamMembers = mutableListOf(
        GrowfoneUserDto(
            id = "4d5e6f70-8192-4a3b-9c4d-5e6f708192a3",
            name = "Sales Agent",
            email = "sales.agent@example.com",
            phoneNumber = "+15416342748",
            credits = 40,
            status = "active",
            enableReminderNotes = true,
            enablePlayRecording = true,
            parentUserId = "9b1f2c34-5d6e-4a7b-8c9d-0e1f2a3b4c5d",
            roleId = "b3c4d5e6-f708-4192-a3b4-c5d6e7f80912",
            createdAt = "2026-08-24T07:52:31.000000Z"
        ),
        GrowfoneUserDto(
            id = "6f708192-a3b4-4c5d-8e6f-708192a3b4c5",
            name = "Support Agent",
            email = "support.agent@example.com",
            phoneNumber = "",
            credits = 0,
            status = "active",
            enableReminderNotes = false,
            enablePlayRecording = false,
            parentUserId = "9b1f2c34-5d6e-4a7b-8c9d-0e1f2a3b4c5d",
            roleId = "7a2b3c4d-5e6f-4071-8293-a4b5c6d7e8f9",
            createdAt = "2026-08-24T07:58:12.000000Z"
        )
    )

    private val feedbacks = mutableListOf(
        FeedbackDto(
            id = "d4e5f607-1829-43a4-b5c6-d7e8f9a0b1c2",
            userId = "9b1f2c34-5d6e-4a7b-8c9d-0e1f2a3b4c5d",
            title = "Call recording playback stutters",
            description = "Recordings longer than 5 minutes stutter on Android 14.",
            type = "bug",
            priority = "high",
            status = "pending",
            createdAt = "2026-08-24T09:02:41.000000Z"
        )
    )

    // ==========================================
    // AUTH IMPLEMENTATION
    // ==========================================

    suspend fun adminLogin(request: LoginRequest): AdminLoginDataDto {
        delay(300)
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

        return AdminLoginDataDto(
            user = currentUser,
            token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.simulated_jwt_token_for_" + currentUser.id
        )
    }

    suspend fun signIn(request: DeviceSignInRequest): GrowfoneUserDto {
        delay(250)
        return currentUser
    }

    suspend fun createProfile(request: CreateProfileRequest): GrowfoneUserDto {
        delay(300)
        currentUser = currentUser.copy(
            id = UUID.randomUUID().toString(),
            name = request.name ?: "New User",
            email = request.email,
            phoneNumber = request.phoneNumber ?: "",
            newLogin = request.newLogin ?: true
        )
        return currentUser
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): SimpleApiResponse {
        delay(200)
        return SimpleApiResponse(success = true, message = "Password reset link sent to ${request.email}")
    }

    suspend fun resetPassword(request: ResetPasswordRequest): SimpleApiResponse {
        delay(200)
        return SimpleApiResponse(success = true, message = "Password has been reset successfully")
    }

    suspend fun getProfile(): GrowfoneUserDto {
        delay(150)
        return currentUser
    }

    suspend fun updateProfile(request: UpdateProfileRequest): GrowfoneUserDto {
        delay(200)
        currentUser = currentUser.copy(
            name = request.name ?: currentUser.name,
            phoneNumber = request.phoneNumber ?: currentUser.phoneNumber,
            newLogin = request.newLogin ?: currentUser.newLogin
        )
        return currentUser
    }

    suspend fun changePassword(request: ChangePasswordRequest): SimpleApiResponse {
        delay(200)
        if (request.newPassword != request.newPasswordConfirmation) {
            throw Exception("The new password confirmation does not match.")
        }
        return SimpleApiResponse(success = true, message = "Password changed successfully")
    }

    suspend fun getCredit(): CreditsDataDto {
        delay(100)
        return CreditsDataDto(credits = currentUser.credits ?: 250)
    }

    // ==========================================
    // COMPANY IMPLEMENTATION
    // ==========================================

    suspend fun getCompany(): CompanyDto {
        delay(150)
        return company
    }

    suspend fun createCompany(request: CreateCompanyRequest): CompanyDto {
        delay(200)
        company = company.copy(
            name = request.name,
            email = request.email,
            website = request.website ?: "",
            mobileNumber = request.mobileNumber ?: "",
            city = request.city ?: "",
            state = request.state ?: ""
        )
        return company
    }

    suspend fun updateCompany(request: UpdateCompanyRequest): CompanyDto {
        delay(200)
        company = company.copy(
            name = request.name ?: company.name,
            website = request.website ?: company.website,
            mobileNumber = request.mobileNumber ?: company.mobileNumber,
            city = request.city ?: company.city,
            state = request.state ?: company.state,
            status = request.status ?: company.status
        )
        return company
    }

    // ==========================================
    // PHONE NUMBERS & TELEPHONY
    // ==========================================

    suspend fun getAreaList(request: AreaListRequest): List<AreaCodeDto> {
        delay(150)
        return listOf(
            AreaCodeDto(id = 412, country = "US", countryName = "United States", areaCode = "541", region = "Oregon"),
            AreaCodeDto(id = 413, country = "US", countryName = "United States", areaCode = "458", region = "Oregon"),
            AreaCodeDto(id = 414, country = "US", countryName = "United States", areaCode = "415", region = "San Francisco"),
            AreaCodeDto(id = 415, country = "US", countryName = "United States", areaCode = "212", region = "New York")
        )
    }

    suspend fun findPhoneNumber(request: FindPhoneNumberRequest): List<AvailableNumberDto> {
        delay(250)
        val area = if (!request.areaCode.isNullOrBlank()) request.areaCode else "541"
        return listOf(
            AvailableNumberDto(friendlyName = "($area) 634-2748", phoneNumber = "+1${area}6342748"),
            AvailableNumberDto(friendlyName = "($area) 634-2751", phoneNumber = "+1${area}6342751"),
            AvailableNumberDto(friendlyName = "($area) 700-1188", phoneNumber = "+1${area}7001188"),
            AvailableNumberDto(friendlyName = "($area) 555-0199", phoneNumber = "+1${area}5550199")
        )
    }

    suspend fun holdNumber(request: HoldNumberRequest): HoldNumberDataDto {
        delay(200)
        return HoldNumberDataDto(
            id = UUID.randomUUID().toString(),
            phoneNumber = request.phoneNumber,
            friendlyName = request.friendlyName,
            isHold = true,
            userId = currentUser.id,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
    }

    suspend fun purchaseNumbers(request: PurchaseNumberRequest): PurchasedNumberDto {
        delay(300)
        currentUser = currentUser.copy(phoneNumber = request.phoneNumber, friendlyName = request.friendlyName)
        return PurchasedNumberDto(
            id = UUID.randomUUID().toString(),
            userId = currentUser.id,
            phoneNumber = request.phoneNumber,
            friendlyName = request.friendlyName,
            sid = "PN" + UUID.randomUUID().toString().replace("-", ""),
            status = 1,
            countryCode = "US",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
    }

    suspend fun getNumberDetails(): PurchasedNumberDto {
        delay(150)
        val num = if (currentUser.phoneNumber.isNullOrBlank()) "+12183061691" else currentUser.phoneNumber!!
        val friendly = if (currentUser.friendlyName.isNullOrBlank()) "(218) 306-1691" else currentUser.friendlyName!!
        return PurchasedNumberDto(
            id = "8c379441-203a-44b0-ba34-600a2bd030a2",
            userId = currentUser.id,
            phoneNumber = num,
            friendlyName = friendly,
            sid = "PN7c338c9fc632096bb38387e6f092b908",
            status = 1,
            countryCode = "US",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
    }

    suspend fun getAllNumberDetails(): List<PurchasedNumberDto> {
        delay(150)
        return listOf(
            getNumberDetails(),
            PurchasedNumberDto(
                id = "9d48a552-314b-45c1-cb45-711b3ce141b3",
                userId = currentUser.id,
                phoneNumber = "+15417001188",
                friendlyName = "(541) 700-1188",
                sid = "PN8d449daf7d743107cc49498f7103ca19",
                status = 1,
                countryCode = "US",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
        )
    }

    suspend fun getCapabilityToken(): CapabilityTokenDto {
        delay(150)
        return CapabilityTokenDto(
            identity = "user_${currentUser.id.take(8)}",
            token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.simulated_capability_token"
        )
    }

    suspend fun getCallLogs(twilioNumber: String? = null): List<CallLogDto> {
        delay(150)
        return callLogs
    }

    suspend fun recordCall(call: CallRecord): CallRecord {
        delay(100)
        val userPhone = currentUser.phoneNumber ?: "+15416342748"
        val log = CallLogDto(
            toPhoneNumber = if (call.direction == CallDirection.OUTGOING) call.remotePhoneNumber else userPhone,
            fromPhoneNumber = if (call.direction == CallDirection.INCOMING || call.direction == CallDirection.MISSED) call.remotePhoneNumber else userPhone,
            startTime = Instant.ofEpochMilli(call.timestamp).toString(),
            duration = call.durationSeconds,
            direction = if (call.direction == CallDirection.OUTGOING) "outbound" else "inbound",
            status = when (call.status) {
                CallRecordStatus.COMPLETED -> "completed"
                CallRecordStatus.BUSY -> "busy"
                CallRecordStatus.NO_ANSWER -> "no-answer"
                CallRecordStatus.FAILED -> "failed"
                CallRecordStatus.CANCELED -> "canceled"
            }
        )
        callLogs.add(0, log)
        return call
    }

    // ==========================================
    // CONTACTS IMPLEMENTATION (CRUD + DND + Blacklist)
    // ==========================================

    suspend fun getContacts(
        page: Int = 1,
        search: String? = null,
        isDnd: Int? = null,
        isBlacklisted: Int? = null
    ): ContactsPagedDto {
        delay(180)
        val filtered = contactsList.filter { contact ->
            val matchSearch = if (!search.isNullOrBlank()) {
                val clean = search.trim()
                (contact.fullName ?: "").contains(clean, ignoreCase = true) ||
                contact.number.contains(clean) ||
                (contact.email ?: "").contains(clean, ignoreCase = true) ||
                (contact.companyName ?: "").contains(clean, ignoreCase = true)
            } else true

            val matchDnd = if (isDnd != null) {
                if (isDnd == 1) contact.isDnd else !contact.isDnd
            } else true

            val matchBlacklist = if (isBlacklisted != null) {
                if (isBlacklisted == 1) contact.isBlacklisted else !contact.isBlacklisted
            } else true

            matchSearch && matchDnd && matchBlacklist
        }

        return ContactsPagedDto(
            currentPage = page,
            data = filtered,
            lastPage = 1,
            perPage = 15,
            total = filtered.size
        )
    }

    suspend fun createContact(request: CreateContactRequest): GrowfoneContactDto {
        delay(200)
        val newId = (System.currentTimeMillis() % 10000 + 50)
        val fullName = if (!request.lastName.isNullOrBlank()) "${request.firstName} ${request.lastName}" else request.firstName
        val created = GrowfoneContactDto(
            id = newId,
            firstName = request.firstName,
            lastName = request.lastName ?: "",
            fullName = fullName,
            number = request.number,
            extension = request.extension ?: "",
            email = request.email ?: "",
            companyName = request.companyName ?: "",
            notes = request.notes ?: "",
            isDnd = request.isDnd ?: false,
            isBlacklisted = request.isBlacklisted ?: false,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        contactsList.add(0, created)
        return created
    }

    suspend fun getContactById(id: Long): GrowfoneContactDto {
        delay(100)
        return contactsList.firstOrNull { it.id == id }
            ?: throw Exception("Contact not found")
    }

    suspend fun updateContact(id: Long, request: CreateContactRequest): GrowfoneContactDto {
        delay(150)
        val index = contactsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = contactsList[index]
            val fullName = if (!request.lastName.isNullOrBlank()) "${request.firstName} ${request.lastName}" else request.firstName
            val updated = existing.copy(
                firstName = request.firstName,
                lastName = request.lastName ?: existing.lastName,
                fullName = fullName,
                number = request.number,
                extension = request.extension ?: existing.extension,
                email = request.email ?: existing.email,
                companyName = request.companyName ?: existing.companyName,
                notes = request.notes ?: existing.notes,
                isDnd = request.isDnd ?: existing.isDnd,
                isBlacklisted = request.isBlacklisted ?: existing.isBlacklisted,
                updatedAt = Instant.now().toString()
            )
            contactsList[index] = updated
            return updated
        } else {
            throw Exception("Contact not found")
        }
    }

    suspend fun deleteContact(id: Long): Boolean {
        delay(100)
        return contactsList.removeAll { it.id == id }
    }

    suspend fun toggleContactDnd(id: Long): GrowfoneContactDto {
        delay(120)
        val index = contactsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = contactsList[index]
            val updated = existing.copy(isDnd = !existing.isDnd, updatedAt = Instant.now().toString())
            contactsList[index] = updated
            return updated
        } else {
            throw Exception("Contact not found")
        }
    }

    suspend fun toggleContactBlacklist(id: Long): GrowfoneContactDto {
        delay(120)
        val index = contactsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = contactsList[index]
            val updated = existing.copy(isBlacklisted = !existing.isBlacklisted, updatedAt = Instant.now().toString())
            contactsList[index] = updated
            return updated
        } else {
            throw Exception("Contact not found")
        }
    }

    // ==========================================
    // TEAM MEMBERS IMPLEMENTATION
    // ==========================================

    suspend fun getAllTeamMembers(): TeamMembersPagedDto {
        delay(150)
        return TeamMembersPagedDto(
            currentPage = 1,
            data = teamMembers,
            lastPage = 1,
            perPage = 10,
            total = teamMembers.size
        )
    }

    suspend fun createTeamMember(request: CreateTeamMemberRequest): GrowfoneUserDto {
        delay(200)
        val newMember = GrowfoneUserDto(
            id = UUID.randomUUID().toString(),
            name = request.name,
            email = request.email,
            phoneNumber = "",
            credits = 50,
            status = request.status ?: "active",
            enableReminderNotes = request.enableReminderNotes ?: true,
            enablePlayRecording = request.enablePlayRecording ?: true,
            parentUserId = currentUser.id,
            roleId = request.roleId ?: "7a2b3c4d-5e6f-4071-8293-a4b5c6d7e8f9",
            createdAt = Instant.now().toString()
        )
        teamMembers.add(0, newMember)
        return newMember
    }

    suspend fun updateTeamMember(request: UpdateTeamMemberRequest): GrowfoneUserDto {
        delay(150)
        val index = teamMembers.indexOfFirst { it.id == request.id }
        if (index != -1) {
            val existing = teamMembers[index]
            val updated = existing.copy(
                name = request.name ?: existing.name,
                email = request.email ?: existing.email,
                status = request.status ?: existing.status,
                roleId = request.roleId ?: existing.roleId
            )
            teamMembers[index] = updated
            return updated
        } else {
            throw Exception("Team member not found")
        }
    }

    suspend fun deleteTeamMember(id: String): Boolean {
        delay(100)
        return teamMembers.removeAll { it.id == id }
    }

    // ==========================================
    // DASHBOARD & FEEDBACK & PLANS
    // ==========================================

    suspend fun getDashboard(): DashboardStatsDto {
        delay(150)
        return DashboardStatsDto(
            totalCalls = 348,
            acceptedCalls = 291,
            rejectedCalls = 22,
            todaysOutgoingCalls = 17,
            todaysFollowUps = 24,
            totalTeamMembers = teamMembers.size,
            topPerformers = listOf(
                TopPerformerDto(id = "4d5e6f70-8192-4a3b-9c4d-5e6f708192a3", name = "Sales Agent", totalCalls = 128),
                TopPerformerDto(id = "6f708192-a3b4-4c5d-8e6f-708192a3b4c5", name = "Support Agent", totalCalls = 96)
            ),
            topDialedCountries = listOf(
                TopDialedCountryDto(toCountryCode = "IN", countryName = "India", totalCalls = 187),
                TopDialedCountryDto(toCountryCode = "US", countryName = "United States", totalCalls = 92),
                TopDialedCountryDto(toCountryCode = "GB", countryName = "United Kingdom", totalCalls = 41)
            )
        )
    }

    suspend fun getUserFeedback(): List<FeedbackDto> {
        delay(100)
        return feedbacks
    }

    suspend fun createFeedback(request: CreateFeedbackRequest): FeedbackDto {
        delay(150)
        val newFeedback = FeedbackDto(
            id = UUID.randomUUID().toString(),
            userId = currentUser.id,
            title = request.title,
            description = request.description,
            type = request.type,
            priority = request.priority,
            status = "pending",
            createdAt = Instant.now().toString()
        )
        feedbacks.add(0, newFeedback)
        return newFeedback
    }

    suspend fun getAllPlans(): AllPlansDataDto {
        delay(150)
        return AllPlansDataDto(
            credits = currentUser.credits ?: 250,
            subscription = listOf(
                PlanDto(
                    id = "f6071829-3a4b-45c6-d7e8-f9a0b1c2d3e4",
                    productId = "Starter Monthly",
                    description = "One number, 500 minutes, single agent",
                    type = "subscription",
                    credits = 500,
                    price = 999.0,
                    actualPrice = 1499.0,
                    discount = 500.0,
                    percentage = 33.36,
                    lists = listOf("1 phone number included", "500 outbound minutes", "Call recording", "Email support")
                ),
                PlanDto(
                    id = "0718293a-4b5c-46d7-e8f9-a0b1c2d3e4f5",
                    productId = "Team Annual",
                    description = "Five numbers, 10000 minutes, unlimited agents",
                    type = "subscription",
                    credits = 10000,
                    price = 8999.0,
                    actualPrice = 14999.0,
                    discount = 6000.0,
                    percentage = 40.0,
                    lists = listOf("5 phone numbers included", "10000 outbound minutes", "Call recording and transcripts", "Priority support")
                )
            )
        )
    }

    suspend fun getTeamSubscriptions(): TeamSubscriptionsDataDto {
        delay(150)
        return TeamSubscriptionsDataDto(
            teamSubscriptions = listOf(
                TeamSubscriptionItemDto(
                    userId = "4d5e6f70-8192-4a3b-9c4d-5e6f708192a3",
                    teamName = "Sales Agent",
                    phoneNumber = currentUser.phoneNumber ?: "+15416342748",
                    friendlyName = currentUser.friendlyName ?: "(541) 634-2748",
                    currentSubscription = CurrentSubscriptionDetailDto(
                        planId = "f6071829-3a4b-45c6-d7e8-f9a0b1c2d3e4",
                        planName = "Starter Monthly",
                        planType = "subscription",
                        status = "active",
                        startDate = "2026-08-01",
                        endDate = "2026-08-31",
                        daysRemaining = 7
                    )
                )
            ),
            subscriptions = getAllPlans().subscription
        )
    }

    suspend fun generatePaymentLink(request: GeneratePaymentLinkRequest): String {
        delay(200)
        return "https://rzp.io/i/simulated_growfone_payment"
    }

    suspend fun getAllInvoices(): InvoicesDataDto {
        delay(150)
        return InvoicesDataDto(
            user = currentUser,
            company = company,
            invoices = listOf(
                InvoiceDto(
                    id = "18293a4b-5c6d-47e8-f9a0-b1c2d3e4f506",
                    razorpayPaymentId = "pay_NzAaBbCcDdEeFf",
                    amount = 999.0,
                    status = "success",
                    userId = currentUser.id,
                    planId = "f6071829-3a4b-45c6-d7e8-f9a0b1c2d3e4",
                    createdAt = "2026-08-01T06:12:44.000000Z"
                )
            )
        )
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
