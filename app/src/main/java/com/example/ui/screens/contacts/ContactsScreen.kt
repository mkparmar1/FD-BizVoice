package com.example.ui.screens.contacts

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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val scope = rememberCoroutineScope()
    val allContacts by repository.allContactsFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactsFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("contacts_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Uniform Top Header
            MainTabHeader(
                title = "Contacts",
                subtitle = if (filteredContacts.isNotEmpty()) "${filteredContacts.size} contacts" else "Directory"
            ) {
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = { Text("Device", fontWeight = if (selectedFilter == ContactsFilter.DEVICE_CONTACTS) FontWeight.Bold else FontWeight.Medium) },
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
                    description = if (searchQuery.isNotBlank()) "No contacts matching '$searchQuery'" else "Add your business clients, partners, or team members.",
                    actionLabel = if (searchQuery.isBlank()) "Add First Contact" else null,
                    onActionClick = onNavigateToAddContact,
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
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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
