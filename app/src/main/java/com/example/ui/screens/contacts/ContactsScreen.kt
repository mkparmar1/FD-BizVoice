package com.example.ui.screens.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Contact
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.ui.components.BizAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MainTabHeader
import com.example.ui.theme.CallGreen
import kotlinx.coroutines.launch

enum class ContactsFilter {
    ALL,
    APP_CONTACTS,
    DEVICE_CONTACTS
}

@Composable
fun ContactsScreen(
    repository: BizVoiceRepository,
    callManager: CallManager,
    onNavigateToContactDetail: (String) -> Unit,
    onNavigateToAddContact: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allContacts by repository.allContactsFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactsFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncBannerMessage by remember { mutableStateOf<String?>(null) }
    var showSyncModal by remember { mutableStateOf(false) }

    val hasContactsPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission.value = isGranted
        if (isGranted) {
            isSyncing = true
            scope.launch {
                val res = repository.syncDeviceContacts()
                isSyncing = false
                if (res.isSuccess) {
                    val count = res.getOrDefault(0)
                    syncBannerMessage = "Successfully synced $count mobile contact(s) into BizVoice"
                } else {
                    syncBannerMessage = res.exceptionOrNull()?.message ?: "Failed to sync mobile contacts"
                }
            }
        } else {
            syncBannerMessage = "Permission denied. Allow Contacts permission in app settings to sync."
        }
    }

    LaunchedEffect(Unit) {
        repository.refreshContacts()
    }

    val filteredContacts = remember(allContacts, searchQuery, selectedFilter) {
        allContacts.filter { contact ->
            val matchesFilter = when (selectedFilter) {
                ContactsFilter.ALL -> true
                ContactsFilter.APP_CONTACTS -> !contact.isDeviceContact
                ContactsFilter.DEVICE_CONTACTS -> contact.isDeviceContact
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                contact.name.contains(searchQuery, ignoreCase = true) ||
                        contact.phoneNumber.contains(searchQuery) ||
                        (contact.organization?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }.sortedBy { it.name }
    }

    val deviceContactCount = remember(allContacts) {
        allContacts.count { it.isDeviceContact }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("contacts_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Uniform Top Header with Sync & Refresh actions
            MainTabHeader(
                title = "Contacts",
                subtitle = if (filteredContacts.isNotEmpty()) "${filteredContacts.size} contacts" else "Directory"
            ) {
                // Sync Mobile Contacts Action Button
                IconButton(
                    onClick = { showSyncModal = true },
                    modifier = Modifier.testTag("sync_mobile_contacts_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Mobile Contacts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Cloud Refresh Action Button
                IconButton(
                    onClick = {
                        isRefreshing = true
                        scope.launch {
                            repository.refreshContacts()
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.testTag("refresh_contacts_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Contacts")
                    }
                }
            }

            // Sync Status Notification Banner
            AnimatedVisibility(visible = syncBannerMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncBannerMessage?.startsWith("Successfully") == true)
                            Color(0xFFDCFCE7)
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (syncBannerMessage?.startsWith("Successfully") == true) Icons.Default.Check else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (syncBannerMessage?.startsWith("Successfully") == true) CallGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncBannerMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (syncBannerMessage?.startsWith("Successfully") == true) Color(0xFF166534) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { syncBannerMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone, company...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("contacts_search_input")
            )

            // Filter Chips + Quick Sync Mobile Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilterChip(
                    selected = selectedFilter == ContactsFilter.ALL,
                    onClick = { selectedFilter = ContactsFilter.ALL },
                    label = { Text("All", fontWeight = if (selectedFilter == ContactsFilter.ALL) FontWeight.Bold else FontWeight.Medium) },
                    colors = chipColors,
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = selectedFilter == ContactsFilter.APP_CONTACTS,
                    onClick = { selectedFilter = ContactsFilter.APP_CONTACTS },
                    label = { Text("Cloud / App", fontWeight = if (selectedFilter == ContactsFilter.APP_CONTACTS) FontWeight.Bold else FontWeight.Medium) },
                    colors = chipColors,
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = selectedFilter == ContactsFilter.DEVICE_CONTACTS,
                    onClick = { selectedFilter = ContactsFilter.DEVICE_CONTACTS },
                    label = {
                        Text(
                            text = if (deviceContactCount > 0) "Mobile ($deviceContactCount)" else "Mobile",
                            fontWeight = if (selectedFilter == ContactsFilter.DEVICE_CONTACTS) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = chipColors,
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            if (filteredContacts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Contacts,
                    title = if (searchQuery.isNotBlank()) "No Contacts Found" else "No Contacts",
                    description = if (searchQuery.isNotBlank())
                        "No contacts matching '$searchQuery'"
                    else if (selectedFilter == ContactsFilter.DEVICE_CONTACTS)
                        "You haven't synced your mobile phonebook contacts into BizVoice yet."
                    else
                        "Add your business clients, or sync your phonebook contacts to call with BizVoice.",
                    actionLabel = if (searchQuery.isBlank() && selectedFilter == ContactsFilter.DEVICE_CONTACTS)
                        "Sync Mobile Contacts"
                    else if (searchQuery.isBlank())
                        "Add First Contact"
                    else null,
                    onActionClick = {
                        if (selectedFilter == ContactsFilter.DEVICE_CONTACTS) {
                            showSyncModal = true
                        } else {
                            onNavigateToAddContact()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItemRow(
                            contact = contact,
                            onClick = { onNavigateToContactDetail(contact.id) },
                            onCallClick = {
                                callManager.startOutgoingCall(contact.phoneNumber, contact.name)
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAddContact,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Contact")
        }
    }

    // SYNC MOBILE CONTACTS DIALOG
    if (showSyncModal) {
        AlertDialog(
            onDismissRequest = { if (!isSyncing) showSyncModal = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Sync Mobile Contacts",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Import phone numbers and names from your mobile device contact directory into BizVoice so you can place VoIP calls and see caller IDs seamlessly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Current Status",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (deviceContactCount > 0)
                                    "• $deviceContactCount mobile contacts synced in BizVoice"
                                else
                                    "• No mobile contacts synced yet",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Mobile contacts stay on your device and are identified with a Mobile tag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            isSyncing = true
                            showSyncModal = false
                            scope.launch {
                                val res = repository.syncDeviceContacts()
                                isSyncing = false
                                if (res.isSuccess) {
                                    val count = res.getOrDefault(0)
                                    syncBannerMessage = "Successfully synced $count mobile contact(s) into BizVoice!"
                                } else {
                                    syncBannerMessage = res.exceptionOrNull()?.message ?: "Failed to sync mobile contacts"
                                }
                            }
                        } else {
                            showSyncModal = false
                            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("confirm_sync_contacts_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (deviceContactCount > 0) "Re-Sync Now" else "Sync Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSyncModal = false },
                    enabled = !isSyncing
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactItemRow(
    contact: Contact,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("contact_item_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            BizAvatar(name = contact.name, size = 44)

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (contact.isDeviceContact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Mobile",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (!contact.organization.isNullOrBlank()) "${contact.phoneNumber} • ${contact.organization}" else contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onCallClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("call_contact_button_${contact.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call ${contact.name}",
                tint = CallGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

