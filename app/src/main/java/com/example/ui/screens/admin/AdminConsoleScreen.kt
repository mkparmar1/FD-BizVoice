package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalyticsOverviewDto
import com.example.data.repository.BizVoiceRepository
import com.example.ui.theme.CallGreen
import com.example.ui.theme.ModernPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToUsers: () -> Unit,
    onNavigateToNumbers: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToContacts: () -> Unit,
    isTabRoot: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by repository.currentUserFlow.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var isTwilioSyncing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var overview by remember { mutableStateOf<AnalyticsOverviewDto?>(null) }
    var lastSyncTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation_anim"
    )

    fun refreshOverview() {
        scope.launch {
            isLoading = true
            errorMessage = null
            repository.getCredits()
            val result = repository.getAdminAnalyticsOverview()
            isLoading = false
            if (result.isSuccess) {
                overview = result.getOrNull()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to load dashboard metrics"
                errorMessage = err
            }
        }
    }

    fun syncTwilio() {
        scope.launch {
            isTwilioSyncing = true
            val syncResult = repository.syncTwilioBalanceAndAccount()
            isTwilioSyncing = false
            if (syncResult.isSuccess) {
                val summary = syncResult.getOrNull()
                lastSyncTime = System.currentTimeMillis()
                val freshCredits = summary?.credits ?: (currentUser?.credits ?: 0)
                val syncedNumbers = summary?.syncedNumbers ?: 0
                snackbarHostState.showSnackbar(
                    "Twilio Synced: Real balance updated (${freshCredits} credits) • $syncedNumbers numbers verified"
                )
                val overviewResult = repository.getAdminAnalyticsOverview()
                if (overviewResult.isSuccess) {
                    overview = overviewResult.getOrNull()
                }
            } else {
                val err = syncResult.exceptionOrNull()?.localizedMessage ?: "Twilio sync failed. Please check network."
                snackbarHostState.showSnackbar(err)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshOverview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Console",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = currentUser?.company ?: "Enterprise Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (isTabRoot) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(start = 12.dp, end = 4.dp)
                                .size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Console",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("admin_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { syncTwilio() },
                        enabled = !isTwilioSyncing,
                        modifier = Modifier.testTag("admin_sync_twilio_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Twilio Balance",
                            tint = if (isTwilioSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = if (isTwilioSyncing) Modifier.rotate(syncRotation) else Modifier
                        )
                    }
                    IconButton(
                        onClick = { refreshOverview() },
                        modifier = Modifier.testTag("admin_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("admin_console_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Twilio Real Balance & Voice Gateway Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("twilio_balance_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header Row: Twilio Voice Gateway Status Badge + Live indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CallGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TWILIO VOICE ENGINE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CallGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = CallGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real Twilio Balance Presentation
                        val currentCredits = currentUser?.credits ?: 0
                        val formattedBalance = String.format(Locale.US, "$%.2f", currentCredits / 100.0)

                        Text(
                            text = "Twilio Real Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = formattedBalance,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("twilio_real_balance_text")
                                )
                                Text(
                                    text = "$currentCredits VoIP Calling Credits Available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Sync Twilio Button
                            Button(
                                onClick = { syncTwilio() },
                                enabled = !isTwilioSyncing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("sync_twilio_balance_button")
                            ) {
                                if (isTwilioSyncing) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Syncing...",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sync",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer with Last Sync Timestamp & Account detail
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val formattedTime = timeFormat.format(Date(lastSyncTime))
                            Text(
                                text = "Last synced: $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "VoIP Live Billing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (overview != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminStatCard(
                                title = "Total Calls",
                                value = (overview!!.totals?.totalCalls ?: 0).toString(),
                                subtitle = "${overview!!.totals?.answeredCalls ?: 0} answered",
                                icon = Icons.Default.Call,
                                color = ModernPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                title = "VoIP Usage",
                                value = "${String.format(Locale.US, "%.0f", overview!!.totals?.totalMinutes ?: 0.0)}m",
                                subtitle = "Active minutes",
                                icon = Icons.Default.Analytics,
                                color = CallGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section: Admin Modules
            item {
                Text(
                    text = "MANAGEMENT MODULES",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AdminModuleRow(
                            icon = Icons.Default.Group,
                            title = "Team Members",
                            subtitle = "Add, edit, activate or manage team user accounts",
                            onClick = onNavigateToUsers,
                            testTag = "admin_users_module_btn"
                        )
                        AdminDivider()
                        AdminModuleRow(
                            icon = Icons.Default.Phone,
                            title = "Phone Numbers & Twilio Inventory",
                            subtitle = "Inventory, user assignments, release & sync",
                            onClick = onNavigateToNumbers,
                            testTag = "admin_numbers_module_btn"
                        )
                        AdminDivider()
                        AdminModuleRow(
                            icon = Icons.Default.Analytics,
                            title = "Analytics & Call Stats",
                            subtitle = "Inbound/Outbound performance and team metrics",
                            onClick = onNavigateToAnalytics,
                            testTag = "admin_analytics_module_btn"
                        )
                        AdminDivider()
                        AdminModuleRow(
                            icon = Icons.Default.Feedback,
                            title = "Feedback Tickets",
                            subtitle = "Review and reply to user inquiries and bug reports",
                            onClick = onNavigateToFeedback,
                            testTag = "admin_feedback_module_btn"
                        )
                        AdminDivider()
                        AdminModuleRow(
                            icon = Icons.Default.Contacts,
                            title = "Shared Contacts & Sync",
                            subtitle = "Organization address book and 1-tap device sync",
                            onClick = onNavigateToContacts,
                            testTag = "admin_contacts_module_btn"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminModuleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AdminDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

