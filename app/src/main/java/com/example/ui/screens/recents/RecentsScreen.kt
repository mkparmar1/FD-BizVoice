package com.example.ui.screens.recents

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.sp
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.ui.components.BizAvatar
import com.example.ui.components.CallDirectionIcon
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MainTabHeader
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecentsFilter {
    ALL,
    INCOMING,
    OUTGOING,
    MISSED
}

@Composable
fun RecentsScreen(
    repository: BizVoiceRepository,
    callManager: CallManager,
    onNavigateToCallDetail: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val allCalls by repository.allCallsFlow.collectAsState()

    var selectedFilter by remember { mutableStateOf(RecentsFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = repository.refreshCalls()
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load call logs"
        }
    }

    val filteredCalls = remember(allCalls, selectedFilter, searchQuery) {
        allCalls.filter { call ->
            val matchesFilter = when (selectedFilter) {
                RecentsFilter.ALL -> true
                RecentsFilter.INCOMING -> call.direction == CallDirection.INCOMING
                RecentsFilter.OUTGOING -> call.direction == CallDirection.OUTGOING
                RecentsFilter.MISSED -> call.direction == CallDirection.MISSED
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                (call.remoteName?.contains(searchQuery, ignoreCase = true) == true) ||
                        call.remotePhoneNumber.contains(searchQuery)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("recents_screen")
    ) {
        // Uniform Top Header
        MainTabHeader(
            title = "Recents",
            subtitle = if (filteredCalls.isNotEmpty()) "${filteredCalls.size} call records" else "Call history"
        ) {
            IconButton(
                onClick = {
                    isRefreshing = true
                    errorMessage = null
                    scope.launch {
                        val result = repository.refreshCalls(forceRefresh = true)
                        if (result.isFailure) {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load call logs"
                        }
                        isRefreshing = false
                    }
                },
                modifier = Modifier.testTag("refresh_recents_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Calls"
                    )
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or number...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                .testTag("recents_search_input")
        )

        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecentsFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Optional Error Banner when calls are cached but latest refresh failed
        if (errorMessage != null && filteredCalls.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "Failed to update call logs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            errorMessage = null
                            scope.launch {
                                val result = repository.refreshCalls(forceRefresh = true)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load call logs"
                                }
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Calls List or Empty / Error State
        if (filteredCalls.isEmpty()) {
            if (errorMessage != null) {
                EmptyStateView(
                    icon = Icons.Default.ErrorOutline,
                    title = "Could Not Load History",
                    description = errorMessage ?: "A parse or network error occurred while loading call logs.",
                    actionLabel = "Retry",
                    onActionClick = {
                        isRefreshing = true
                        errorMessage = null
                        scope.launch {
                            val result = repository.refreshCalls(forceRefresh = true)
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load call logs"
                            }
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmptyStateView(
                    icon = Icons.Default.History,
                    title = if (searchQuery.isNotBlank()) "No Matching Calls" else "No Calls Yet",
                    description = if (searchQuery.isNotBlank()) "No records found matching '$searchQuery'" else "Your outgoing, incoming, and missed VoIP calls will appear here.",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    CallHistoryItem(
                        call = call,
                        onClick = { onNavigateToCallDetail(call.id) },
                        onCallAgain = {
                            callManager.startOutgoingCall(call.remotePhoneNumber, call.remoteName)
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
}

@Composable
private fun CallHistoryItem(
    call: CallRecord,
    onClick: () -> Unit,
    onCallAgain: () -> Unit
) {
    val formattedDate = remember(call.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(call.timestamp))
    }

    val formattedDuration = remember(call.durationSeconds, call.status, call.direction) {
        when {
            call.status == CallRecordStatus.NO_ANSWER -> "No Answer"
            call.status == CallRecordStatus.BUSY -> "Busy"
            call.status == CallRecordStatus.FAILED -> "Failed"
            call.direction == CallDirection.MISSED || (call.durationSeconds == 0L && call.direction == CallDirection.INCOMING) -> "Missed"
            call.durationSeconds == 0L -> "Unanswered"
            else -> {
                val mins = call.durationSeconds / 60
                val secs = call.durationSeconds % 60
                String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("call_history_item_${call.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            BizAvatar(
                name = call.remoteName ?: call.remotePhoneNumber,
                size = 44
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = call.remoteName ?: call.remotePhoneNumber,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (call.direction == CallDirection.MISSED) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (call.direction == CallDirection.MISSED) CallRed else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallDirectionIcon(direction = call.direction)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedDuration • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Call Button
        IconButton(
            onClick = onCallAgain,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("call_again_button_${call.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call Again",
                tint = CallGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
