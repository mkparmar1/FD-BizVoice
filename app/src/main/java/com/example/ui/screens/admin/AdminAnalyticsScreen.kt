package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.AdminCallDto
import com.example.data.model.AdminUserDto
import com.example.data.model.AnalyticsOverviewDto
import com.example.data.model.UserMetricDto
import com.example.data.repository.BizVoiceRepository
import com.example.ui.components.BizAvatar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.ModernPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var overview by remember { mutableStateOf<AnalyticsOverviewDto?>(null) }
    var userMetrics by remember { mutableStateOf<List<UserMetricDto>>(emptyList()) }
    var adminUsers by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedUserForCalls by remember { mutableStateOf<UserMetricDto?>(null) }

    fun loadAnalytics() {
        scope.launch {
            isLoading = true
            val overviewResult = repository.getAdminAnalyticsOverview()
            val usersResult = repository.getAdminAnalyticsUsers(perPage = 50)
            val rosterResult = repository.getAdminUsers(perPage = 50)
            isLoading = false

            if (overviewResult.isSuccess) {
                overview = overviewResult.getOrNull()
            }
            if (usersResult.isSuccess) {
                userMetrics = usersResult.getOrNull()?.data ?: emptyList()
            }
            if (rosterResult.isSuccess) {
                adminUsers = rosterResult.getOrNull()?.data ?: emptyList()
            }

            if (overviewResult.isFailure && usersResult.isFailure) {
                snackbarHostState.showSnackbar("Failed to load analytics data")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics & Stats",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_analytics_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadAnalytics() }) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("admin_analytics_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Metrics
                item {
                    Text(
                        text = "ORGANIZATION CALL VOLUME",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            title = "Total Calls",
                            value = (overview?.totals?.totalCalls ?: 0).toString(),
                            subtitle = "${overview?.totals?.answeredCalls ?: 0} completed",
                            icon = Icons.Default.Call,
                            color = ModernPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Duration",
                            value = "${String.format("%.0f", overview?.totals?.totalMinutes ?: 0.0)}m",
                            subtitle = "${String.format("%.0f", overview?.totals?.avgDurationSeconds ?: 0.0)}s avg duration",
                            icon = Icons.Default.Schedule,
                            color = CallGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            title = "Inbound Calls",
                            value = (overview?.totals?.inboundCalls ?: 0).toString(),
                            subtitle = "Received by team",
                            icon = Icons.Default.CallReceived,
                            color = ModernPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Outbound Calls",
                            value = (overview?.totals?.outboundCalls ?: 0).toString(),
                            subtitle = "Placed by team",
                            icon = Icons.Default.CallMade,
                            color = ModernPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Team Breakdown Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEAM MEMBER ACTIVITY",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap user to view call history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (userMetrics.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No user metrics recorded yet for this period.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(userMetrics, key = { it.userId ?: it.name ?: "" }) { metric ->
                        // Match with admin roster to get assigned number if not in metric
                        val matchedRosterUser = adminUsers.firstOrNull { it.id == metric.userId }
                        val resolvedPhoneNumber = metric.assignedPhoneNumber
                            ?: metric.phoneNumber
                            ?: matchedRosterUser?.assignedNumbers?.firstOrNull()?.phoneNumber
                            ?: matchedRosterUser?.phoneNumber

                        UserMetricCard(
                            metric = metric,
                            resolvedPhoneNumber = resolvedPhoneNumber,
                            onClick = {
                                selectedUserForCalls = metric
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for User Call Logs History
    if (selectedUserForCalls != null) {
        val user = selectedUserForCalls!!
        val matchedUser = adminUsers.firstOrNull { it.id == user.userId }
        val userPhone = user.assignedPhoneNumber
            ?: user.phoneNumber
            ?: matchedUser?.assignedNumbers?.firstOrNull()?.phoneNumber
            ?: matchedUser?.phoneNumber

        UserCallHistoryBottomSheet(
            user = user,
            phoneNumber = userPhone,
            repository = repository,
            onDismiss = { selectedUserForCalls = null }
        )
    }
}

@Composable
fun UserMetricCard(
    metric: UserMetricDto,
    resolvedPhoneNumber: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BizAvatar(name = metric.name ?: "User", size = 44)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metric.name ?: "Team Member",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!resolvedPhoneNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = resolvedPhoneNumber,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (!metric.email.isNullOrBlank()) {
                        Text(
                            text = metric.email,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "${metric.totalCalls ?: 0} calls",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onClick() }
                    ) {
                        Text(
                            text = "View Logs",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.0f", metric.totalMinutes ?: 0.0)} mins", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Column {
                    Text("Inbound", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${metric.inboundCalls ?: 0}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = ModernPrimary))
                }
                Column {
                    Text("Outbound", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${metric.outboundCalls ?: 0}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = ModernPrimary))
                }
                Column {
                    Text("Answer Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val rate = if ((metric.totalCalls ?: 0) > 0) {
                        ((metric.answeredCalls ?: 0) * 100 / metric.totalCalls!!)
                    } else 0
                    Text("$rate%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = CallGreen))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCallHistoryBottomSheet(
    user: UserMetricDto,
    phoneNumber: String? = null,
    repository: BizVoiceRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var calls by remember { mutableStateOf<List<AdminCallDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDirectionFilter by remember { mutableStateOf<String?>(null) } // null = All, "inbound", "outbound"
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadUserCalls() {
        val uid = user.userId ?: return
        scope.launch {
            isLoading = true
            errorMessage = null
            val res = repository.getAdminUserCalls(
                id = uid,
                direction = selectedDirectionFilter,
                perPage = 50
            )
            isLoading = false
            if (res.isSuccess) {
                calls = res.getOrNull()?.data ?: emptyList()
            } else {
                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to load call history"
            }
        }
    }

    LaunchedEffect(selectedDirectionFilter) {
        loadUserCalls()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BizAvatar(name = user.name ?: "User", size = 44)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.name ?: "Team Member",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!phoneNumber.isNullOrBlank()) {
                            Text(
                                text = "Line: $phoneNumber",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (!user.email.isNullOrBlank()) {
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direction Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDirectionFilter == null,
                    onClick = { selectedDirectionFilter = null },
                    label = { Text("All Calls (${user.totalCalls ?: 0})") }
                )
                FilterChip(
                    selected = selectedDirectionFilter == "inbound",
                    onClick = { selectedDirectionFilter = "inbound" },
                    label = { Text("Inbound (${user.inboundCalls ?: 0})") },
                    leadingIcon = {
                        Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                FilterChip(
                    selected = selectedDirectionFilter == "outbound",
                    onClick = { selectedDirectionFilter = "outbound" },
                    label = { Text("Outbound (${user.outboundCalls ?: 0})") },
                    leadingIcon = {
                        Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by phone number") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            val displayedCalls = remember(calls, searchQuery) {
                if (searchQuery.isBlank()) calls
                else calls.filter { c ->
                    (c.fromPhoneNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                    (c.toPhoneNumber?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Error loading call logs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadUserCalls() }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (displayedCalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No call records found for this user",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(displayedCalls, key = { it.id ?: it.callSid ?: "${it.fromPhoneNumber}_${it.toPhoneNumber}_${it.createdAt}" }) { call ->
                        AdminCallItemCard(call = call, userPhone = phoneNumber)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCallItemCard(
    call: AdminCallDto,
    userPhone: String? = null
) {
    val isInbound = call.direction?.lowercase() == "inbound"
    val isCompleted = call.status?.lowercase() == "completed"
    val counterParty = if (isInbound) call.fromPhoneNumber else call.toPhoneNumber

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isInbound) CallGreen.copy(alpha = 0.15f) else ModernPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isInbound) Icons.Default.CallReceived else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (isInbound) CallGreen else ModernPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = counterParty ?: "Unknown Number",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isInbound) "From: ${call.fromPhoneNumber ?: "Caller"}" else "To: ${call.toPhoneNumber ?: "Recipient"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!call.createdAt.isNullOrBlank()) {
                    Text(
                        text = call.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCompleted) CallGreen.copy(alpha = 0.12f) else CallRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = call.status?.replaceFirstChar { it.uppercase() } ?: "Completed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCompleted) CallGreen else CallRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = call.durationLabel ?: "${call.duration ?: 0}s",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
