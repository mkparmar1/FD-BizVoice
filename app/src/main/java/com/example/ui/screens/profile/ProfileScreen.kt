package com.example.ui.screens.profile

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.BizVoiceRepository
import com.example.ui.components.BizAvatar
import com.example.ui.components.BizTopAppBar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit,
    onLogoutComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUserFlow.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.refreshUserData()
        repository.refreshAssignedPhoneNumber()
    }

    Scaffold(
        topBar = {
            BizTopAppBar(
                title = "Profile & Account",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            errorMessage = null
                            successMessage = null
                            scope.launch {
                                repository.refreshUserData()
                                repository.refreshAssignedPhoneNumber()
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.testTag("refresh_profile_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Profile")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val user = currentUser
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No user session active", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success / Error alerts
                AnimatedVisibility(visible = successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = CallGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF166534),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Avatar and User Info Header
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BizAvatar(name = user.name, size = 92)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${user.role ?: "Agent"} • ${user.company ?: "BizVoice Enterprise"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Availability Status Selector
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PRESENCE & AVAILABILITY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(
                                label = "Available",
                                color = Color(0xFF22C55E),
                                isSelected = user.status.equals("active", ignoreCase = true) || user.status.equals("available", ignoreCase = true),
                                onClick = {
                                    scope.launch {
                                        repository.updateProfile(
                                            name = user.name,
                                            email = user.email,
                                            assignedPhoneNumber = user.assignedPhoneNumber,
                                            company = user.company,
                                            role = user.role,
                                            status = "active"
                                        )
                                        successMessage = "Status updated to Available"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            StatusChip(
                                label = "Busy",
                                color = Color(0xFFEF4444),
                                isSelected = user.status.equals("busy", ignoreCase = true),
                                onClick = {
                                    scope.launch {
                                        repository.updateProfile(
                                            name = user.name,
                                            email = user.email,
                                            assignedPhoneNumber = user.assignedPhoneNumber,
                                            company = user.company,
                                            role = user.role,
                                            status = "busy"
                                        )
                                        successMessage = "Status updated to Busy"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            StatusChip(
                                label = "Away",
                                color = Color(0xFFF59E0B),
                                isSelected = user.status.equals("away", ignoreCase = true),
                                onClick = {
                                    scope.launch {
                                        repository.updateProfile(
                                            name = user.name,
                                            email = user.email,
                                            assignedPhoneNumber = user.assignedPhoneNumber,
                                            company = user.company,
                                            role = user.role,
                                            status = "away"
                                        )
                                        successMessage = "Status updated to Away"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assigned Business Number Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASSIGNED BUSINESS NUMBER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (!user.assignedPhoneNumber.isNullOrBlank()) "Active Line" else "Unassigned",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = user.assignedPhoneNumber ?: "No phone number assigned",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VoIP caller ID for all outbound & incoming calls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Information Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ACCOUNT DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileInfoRow(
                            icon = Icons.Default.Person,
                            label = "Full Name",
                            value = user.name
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileInfoRow(
                            icon = Icons.Default.Email,
                            label = "Work Email",
                            value = user.email
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileInfoRow(
                            icon = Icons.Default.Work,
                            label = "Job Role",
                            value = user.role ?: "Agent"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileInfoRow(
                            icon = Icons.Default.Business,
                            label = "Organization / Company",
                            value = user.company ?: "BizVoice Enterprise"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ProfileInfoRow(
                            icon = Icons.Default.VerifiedUser,
                            label = "Account Status",
                            value = user.status.replaceFirstChar { it.uppercase() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button: Log Out
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out from BizVoice", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // EDIT PROFILE DIALOG
    if (showEditDialog && currentUser != null) {
        val user = currentUser!!
        var editName by remember { mutableStateOf(user.name) }
        var editRole by remember { mutableStateOf(user.role ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    if (dialogError != null) {
                        Text(
                            text = dialogError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 1. Full Name (Editable)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; dialogError = null },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Job Role (Editable)
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Job Role / Title") },
                        placeholder = { Text("e.g. Sales Executive, Support Agent") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_role_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Work Email (Disabled / Read-only)
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Work Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("Email is managed by your administrator.") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Assigned Phone / Line (Disabled / Read-only)
                    OutlinedTextField(
                        value = user.assignedPhoneNumber ?: "No line assigned",
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Assigned Phone / Line") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("VoIP line assigned by system admin.") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank()) {
                            dialogError = "Full Name cannot be empty"
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val res = repository.updateProfile(
                                name = editName.trim(),
                                email = user.email,
                                assignedPhoneNumber = user.assignedPhoneNumber,
                                company = user.company,
                                role = editRole.trim().ifBlank { null },
                                status = user.status
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                successMessage = "Profile updated successfully!"
                                showEditDialog = false
                            } else {
                                dialogError = res.exceptionOrNull()?.message ?: "Failed to update profile"
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // LOGOUT CONFIRMATION DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("You will be signed out of BizVoice and will need to enter your email and password to log in again.") },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingOut = true
                        scope.launch {
                            repository.logout()
                            isLoggingOut = false
                            showLogoutDialog = false
                            onLogoutComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Log Out")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !isLoggingOut
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
