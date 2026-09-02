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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminNumberDto
import com.example.data.model.AdminUserDto
import com.example.data.repository.BizVoiceRepository
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNumbersScreen(
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var numbers by remember { mutableStateOf<List<AdminNumberDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedAssignedFilter by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Assigned, false = Unassigned

    var numberToAssign by remember { mutableStateOf<AdminNumberDto?>(null) }
    var numberToRelease by remember { mutableStateOf<AdminNumberDto?>(null) }

    fun loadNumbers() {
        scope.launch {
            isLoading = true
            val result = repository.getAdminNumbers(
                search = searchQuery.ifBlank { null },
                isAssigned = selectedAssignedFilter
            )
            isLoading = false
            if (result.isSuccess) {
                numbers = result.getOrNull()?.data ?: emptyList()
            } else {
                snackbarHostState.showSnackbar(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to load numbers"
                )
            }
        }
    }

    fun loadUsersList() {
        scope.launch {
            val res = repository.getAdminUsers(perPage = 50)
            if (res.isSuccess) {
                users = res.getOrNull()?.data ?: emptyList()
            }
        }
    }

    LaunchedEffect(selectedAssignedFilter) {
        loadNumbers()
        loadUsersList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Phone Numbers",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_numbers_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val res = repository.syncAdminNumbers()
                                isSyncing = false
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Numbers synced with Twilio")
                                    loadNumbers()
                                } else {
                                    snackbarHostState.showSnackbar(
                                        res.exceptionOrNull()?.localizedMessage ?: "Sync failed"
                                    )
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync with Twilio")
                        }
                    }
                    IconButton(onClick = { loadNumbers() }) {
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by phone number or name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            loadNumbers()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("admin_numbers_search_input")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedAssignedFilter == null,
                    onClick = { selectedAssignedFilter = null },
                    label = { Text("All Numbers") }
                )
                FilterChip(
                    selected = selectedAssignedFilter == true,
                    onClick = { selectedAssignedFilter = true },
                    label = { Text("Assigned") }
                )
                FilterChip(
                    selected = selectedAssignedFilter == false,
                    onClick = { selectedAssignedFilter = false },
                    label = { Text("Unassigned") }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (numbers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No phone numbers found",
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
                    items(numbers, key = { it.id ?: it.phoneNumber ?: "" }) { num ->
                        AdminNumberCard(
                            number = num,
                            users = users,
                            onAssignClick = { numberToAssign = num },
                            onUnassignClick = {
                                scope.launch {
                                    val nid = num.id ?: return@launch
                                    val res = repository.unassignAdminNumber(nid)
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Number unassigned")
                                        loadNumbers()
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            res.exceptionOrNull()?.localizedMessage ?: "Unassign failed"
                                        )
                                    }
                                }
                            },
                            onReleaseClick = { numberToRelease = num }
                        )
                    }
                }
            }
        }
    }

    // Assign Dialog
    if (numberToAssign != null) {
        AssignUserDialog(
            number = numberToAssign!!,
            users = users,
            onDismiss = { numberToAssign = null },
            onAssign = { userId ->
                scope.launch {
                    val nid = numberToAssign?.id ?: return@launch
                    val res = repository.assignAdminNumber(nid, userId)
                    numberToAssign = null
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Number assigned successfully")
                        loadNumbers()
                    } else {
                        snackbarHostState.showSnackbar(
                            res.exceptionOrNull()?.localizedMessage ?: "Assignment failed"
                        )
                    }
                }
            }
        )
    }

    // Release Confirmation Dialog
    if (numberToRelease != null) {
        AlertDialog(
            onDismissRequest = { numberToRelease = null },
            title = { Text("Release Phone Number?") },
            text = {
                Text(
                    "Releasing ${numberToRelease?.phoneNumber} is permanent and will remove it from your organization inventory."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val nid = numberToRelease?.id ?: return@launch
                            val res = repository.releaseAdminNumber(nid)
                            numberToRelease = null
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Number released successfully")
                                loadNumbers()
                            } else {
                                snackbarHostState.showSnackbar(
                                    res.exceptionOrNull()?.localizedMessage ?: "Release failed"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallRed)
                ) {
                    Text("Confirm Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { numberToRelease = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminNumberCard(
    number: AdminNumberDto,
    users: List<AdminUserDto> = emptyList(),
    onAssignClick: () -> Unit,
    onUnassignClick: () -> Unit,
    onReleaseClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    
    // Resolve assigned user from embedded object, userId lookup, or matching assigned numbers
    val matchedUser = users.firstOrNull { u ->
        (number.userId != null && u.id == number.userId) ||
        (number.assignedUser?.id != null && u.id == number.assignedUser.id) ||
        (number.user?.id != null && u.id == number.user.id) ||
        (!number.phoneNumber.isNullOrBlank() && (u.assignedNumbers?.any { it.phoneNumber == number.phoneNumber } == true || u.phoneNumber == number.phoneNumber))
    }

    val assignedUserName = number.assignedUser?.name?.ifBlank { null }
        ?: number.user?.name?.ifBlank { null }
        ?: number.userName?.ifBlank { null }
        ?: matchedUser?.name?.ifBlank { null }

    val assignedUserEmail = number.assignedUser?.email?.ifBlank { null }
        ?: number.user?.email?.ifBlank { null }
        ?: matchedUser?.email?.ifBlank { null }

    val isAssigned = !assignedUserName.isNullOrBlank() ||
            !assignedUserEmail.isNullOrBlank() ||
            number.assignedUser != null ||
            number.user != null ||
            number.isAssigned == true ||
            matchedUser != null

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
            Surface(
                shape = CircleShape,
                color = if (isAssigned) CallGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (isAssigned) CallGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = number.phoneNumber ?: "Unknown Number",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!number.friendlyName.isNullOrBlank()) {
                    Text(
                        text = number.friendlyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isAssigned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CallGreen.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentInd,
                                    contentDescription = null,
                                    tint = CallGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Assigned to: ${assignedUserName ?: assignedUserEmail ?: "Team Member"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CallGreen
                                )
                            }
                        }
                    }
                    if (!assignedUserEmail.isNullOrBlank() && assignedUserName != null && assignedUserEmail != assignedUserName) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = assignedUserEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Unassigned (Available in Pool)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (!isAssigned) {
                        DropdownMenuItem(
                            text = { Text("Assign to User") },
                            onClick = {
                                menuExpanded = false
                                onAssignClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Unassign Number") },
                            onClick = {
                                menuExpanded = false
                                onUnassignClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Release Number", color = CallRed) },
                        onClick = {
                            menuExpanded = false
                            onReleaseClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AssignUserDialog(
    number: AdminNumberDto,
    users: List<AdminUserDto>,
    onDismiss: () -> Unit,
    onAssign: (userId: String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign ${number.phoneNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a team member:", style = MaterialTheme.typography.bodyMedium)
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(users, key = { it.id ?: it.email ?: "" }) { u ->
                        val isSelected = selectedUserId == u.id
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedUserId = u.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.name ?: "User", style = MaterialTheme.typography.titleSmall)
                                    Text(u.email ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedUserId?.let { onAssign(it) } },
                enabled = selectedUserId != null
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
