package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminContactDto
import com.example.data.model.AdminUserDto
import com.example.data.model.ContactsSummaryDto
import com.example.data.model.SyncContactsSummaryStatsDto
import com.example.data.model.UserContactSummaryDto
import com.example.data.model.UserMetricDto
import com.example.data.repository.BizVoiceRepository
import com.example.ui.components.BizAvatar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.ModernPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContactsScreen(
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var summary by remember { mutableStateOf<ContactsSummaryDto?>(null) }
    var contacts by remember { mutableStateOf<List<AdminContactDto>>(emptyList()) }
    var adminUsers by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedUserForCallsModal by remember { mutableStateOf<UserMetricDto?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var syncResultDialog by remember { mutableStateOf<SyncContactsSummaryStatsDto?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            val summaryRes = repository.getAdminContactsSummary()
            val rosterRes = repository.getAdminUsers(perPage = 50)
            val contactsRes = repository.getAdminContacts(
                userId = selectedUserId,
                search = searchQuery.ifBlank { null }
            )
            isLoading = false

            if (summaryRes.isSuccess) {
                summary = summaryRes.getOrNull()
            }
            if (rosterRes.isSuccess) {
                adminUsers = rosterRes.getOrNull()?.data ?: emptyList()
            }
            if (contactsRes.isSuccess) {
                contacts = contactsRes.getOrNull()?.data ?: emptyList()
            }
        }
    }

    LaunchedEffect(selectedUserId, searchQuery) {
        loadData()
    }

    val selectedUserSummary = remember(selectedUserId, summary) {
        summary?.users?.firstOrNull { it.userId == selectedUserId }
    }
    val selectedAdminUser = remember(selectedUserId, adminUsers) {
        adminUsers.firstOrNull { it.id == selectedUserId }
    }
    val selectedUserPhone = remember(selectedUserSummary, selectedAdminUser) {
        selectedUserSummary?.assignedPhoneNumber
            ?: selectedUserSummary?.phoneNumber
            ?: selectedAdminUser?.assignedNumbers?.firstOrNull()?.phoneNumber
            ?: selectedAdminUser?.phoneNumber
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contacts & Cloud Sync",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_contacts_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cloud Sync Banner Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync Device Contacts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Securely upload & merge address books to the organization cloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val res = repository.syncDeviceContactsToBackend()
                                isSyncing = false
                                if (res.isSuccess) {
                                    syncResultDialog = res.getOrNull()
                                    loadData()
                                } else {
                                    snackbarHostState.showSnackbar(
                                        res.exceptionOrNull()?.localizedMessage ?: "Sync failed"
                                    )
                                }
                            }
                        },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("admin_sync_contacts_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync")
                        }
                    }
                }
            }

            // User Selection Filter Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "FILTER BY TEAM MEMBER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = selectedUserId == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { selectedUserId = null },
                        label = { Text("All Team (${summary?.totalAssigned ?: contacts.size})") },
                        leadingIcon = {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    summary?.users?.forEach { userSummary ->
                        val isUserSelected = selectedUserId == userSummary.userId
                        val matchedRoster = adminUsers.firstOrNull { it.id == userSummary.userId }
                        val line = userSummary.assignedPhoneNumber
                            ?: userSummary.phoneNumber
                            ?: matchedRoster?.assignedNumbers?.firstOrNull()?.phoneNumber
                            ?: matchedRoster?.phoneNumber

                        val chipLabel = buildString {
                            append(userSummary.name.orEmpty().ifBlank { "User" })
                            append(" (${userSummary.contactsCount ?: 0})")
                            if (!line.isNullOrBlank()) {
                                append(" • $line")
                            }
                        }

                        FilterChip(
                            selected = isUserSelected,
                            onClick = { selectedUserId = userSummary.userId },
                            label = { Text(chipLabel) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // If a specific user is selected, show their identity & phone card
            if (selectedUserId != null && (selectedUserSummary != null || selectedAdminUser != null)) {
                val userName = selectedUserSummary?.name?.ifBlank { null } ?: selectedAdminUser?.name ?: "Team Member"
                val userEmail = selectedUserSummary?.email?.ifBlank { null } ?: selectedAdminUser?.email ?: ""
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BizAvatar(name = userName, size = 40)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!selectedUserPhone.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Assigned Line: $selectedUserPhone",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Button to view user's call logs
                        Button(
                            onClick = {
                                selectedUserForCallsModal = UserMetricDto(
                                    userId = selectedUserId,
                                    name = userName,
                                    email = userEmail,
                                    phoneNumber = selectedUserPhone,
                                    assignedPhoneNumber = selectedUserPhone,
                                    totalCalls = 0
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call Logs", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = { Text("Search by contact name, phone, or company") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No contacts matching '$searchQuery'"
                            else if (selectedUserId != null) "No contacts synced for this user"
                            else "No shared contacts found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(contacts, key = { it.id ?: it.number ?: "" }) { c ->
                        val matchedOwner = adminUsers.firstOrNull { it.id == c.userId }
                        val ownerLine = matchedOwner?.assignedNumbers?.firstOrNull()?.phoneNumber
                            ?: matchedOwner?.phoneNumber

                        AdminContactCard(
                            contact = c,
                            ownerUser = matchedOwner,
                            ownerLine = ownerLine
                        )
                    }
                }
            }
        }
    }

    if (syncResultDialog != null) {
        AlertDialog(
            onDismissRequest = { syncResultDialog = null },
            title = { Text("Contacts Sync Results") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Created: ${syncResultDialog!!.created} new contacts")
                    Text("• Claimed: ${syncResultDialog!!.claimed} matched contacts")
                    Text("• Existing: ${syncResultDialog!!.existing} unchanged")
                    if ((syncResultDialog!!.conflicts ?: 0) > 0) {
                        Text("• Conflicts resolved: ${syncResultDialog!!.conflicts}")
                    }
                    Text("• Total in Cloud: ${syncResultDialog!!.totalOwned}")
                }
            },
            confirmButton = {
                Button(onClick = { syncResultDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Modal Sheet for User Call Logs History if invoked
    if (selectedUserForCallsModal != null) {
        val user = selectedUserForCallsModal!!
        UserCallHistoryBottomSheet(
            user = user,
            phoneNumber = user.assignedPhoneNumber ?: user.phoneNumber,
            repository = repository,
            onDismiss = { selectedUserForCallsModal = null }
        )
    }
}

@Composable
fun AdminContactCard(
    contact: AdminContactDto,
    ownerUser: AdminUserDto? = null,
    ownerLine: String? = null
) {
    val fullName = listOfNotNull(contact.firstName, contact.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Contact" }

    val ownerDisplayName = contact.ownerName
        ?: ownerUser?.name
        ?: ownerUser?.email

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BizAvatar(name = fullName, size = 44)
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (contact.isDnd == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CallRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DoNotDisturb, contentDescription = null, tint = CallRed, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("DND", style = MaterialTheme.typography.labelSmall, color = CallRed, fontSize = 9.sp)
                            }
                        }
                    }
                    if (contact.isBlacklisted == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Blacklisted",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                if (!contact.number.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!contact.companyName.isNullOrBlank()) {
                    Text(
                        text = contact.companyName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!ownerDisplayName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = buildString {
                                append("Synced by: $ownerDisplayName")
                                if (!ownerLine.isNullOrBlank()) {
                                    append(" • Line: $ownerLine")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
