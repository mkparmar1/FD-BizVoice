package com.example.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BizVoiceAppContainer
import com.example.data.model.CallDirection
import com.example.telephony.CallState
import com.example.ui.navigation.MainTab
import com.example.ui.screens.admin.AdminConsoleScreen
import com.example.ui.screens.calls.ActiveCallScreen
import com.example.ui.screens.calls.IncomingCallScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.dialer.DialerScreen
import com.example.ui.screens.recents.RecentsScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun MainContainerScreen(
    appContainer: BizVoiceAppContainer,
    onNavigateToCallDetail: (String) -> Unit,
    onNavigateToContactDetail: (String) -> Unit,
    onNavigateToAddContact: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToBackendConfig: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAdminConsole: () -> Unit = {},
    onNavigateToAdminUsers: () -> Unit = {},
    onNavigateToAdminNumbers: () -> Unit = {},
    onNavigateToAdminAnalytics: () -> Unit = {},
    onNavigateToAdminFeedback: () -> Unit = {},
    onNavigateToAdminContacts: () -> Unit = {},
    onLogoutComplete: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.DIALER) }
    val activeCall by appContainer.callManager.activeCallFlow.collectAsState()
    val currentUser by appContainer.repository.currentUserFlow.collectAsState()
    val isAdminRole by appContainer.repository.isAdminFlow.collectAsState()

    val isUserAdmin = isAdminRole ||
            currentUser?.roleSlug == "admin" ||
            currentUser?.roleSlug == "super-admin" ||
            currentUser?.role?.lowercase()?.contains("admin") == true ||
            appContainer.sessionManager.isAdmin()

    LaunchedEffect(isUserAdmin) {
        if (!isUserAdmin && currentTab == MainTab.ADMIN) {
            currentTab = MainTab.DIALER
        }
    }

    LaunchedEffect(Unit) {
        appContainer.repository.refreshAssignedPhoneNumber()
    }

    val isIncomingRinging = activeCall.state == CallState.RINGING && activeCall.direction == CallDirection.INCOMING
    val isActiveCallVisible = activeCall.state != CallState.IDLE && activeCall.state != CallState.ENDED && !isIncomingRinging

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.DIALER,
                        onClick = { currentTab = MainTab.DIALER },
                        icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
                        label = { Text("Keypad", fontWeight = if (currentTab == MainTab.DIALER) FontWeight.Bold else FontWeight.Medium) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_item_dialer")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.RECENTS,
                        onClick = { currentTab = MainTab.RECENTS },
                        icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                        label = { Text("Recents", fontWeight = if (currentTab == MainTab.RECENTS) FontWeight.Bold else FontWeight.Medium) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_item_recents")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.CONTACTS,
                        onClick = { currentTab = MainTab.CONTACTS },
                        icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                        label = { Text("Contacts", fontWeight = if (currentTab == MainTab.CONTACTS) FontWeight.Bold else FontWeight.Medium) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_item_contacts")
                    )
                    if (isUserAdmin) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.ADMIN,
                            onClick = { currentTab = MainTab.ADMIN },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                            label = { Text("Admin", fontWeight = if (currentTab == MainTab.ADMIN) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_item_admin")
                        )
                    }
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Medium) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.DIALER -> DialerScreen(
                        repository = appContainer.repository,
                        callManager = appContainer.callManager,
                        onNavigateToContact = onNavigateToContactDetail
                    )
                    MainTab.RECENTS -> RecentsScreen(
                        repository = appContainer.repository,
                        callManager = appContainer.callManager,
                        onNavigateToCallDetail = onNavigateToCallDetail
                    )
                    MainTab.CONTACTS -> ContactsScreen(
                        repository = appContainer.repository,
                        callManager = appContainer.callManager,
                        onNavigateToContactDetail = onNavigateToContactDetail,
                        onNavigateToAddContact = onNavigateToAddContact
                    )
                    MainTab.ADMIN -> AdminConsoleScreen(
                        repository = appContainer.repository,
                        onNavigateBack = { currentTab = MainTab.DIALER },
                        onNavigateToUsers = onNavigateToAdminUsers,
                        onNavigateToNumbers = onNavigateToAdminNumbers,
                        onNavigateToAnalytics = onNavigateToAdminAnalytics,
                        onNavigateToFeedback = onNavigateToAdminFeedback,
                        onNavigateToContacts = onNavigateToAdminContacts,
                        isTabRoot = true
                    )
                    MainTab.SETTINGS -> SettingsScreen(
                        repository = appContainer.repository,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToBackendConfig = onNavigateToBackendConfig,
                        onNavigateToAbout = onNavigateToAbout,
                        onNavigateToAdminConsole = onNavigateToAdminConsole,
                        onLogoutComplete = onLogoutComplete
                    )
                }
            }
        }

        // Overlay: Incoming Call Screen
        AnimatedVisibility(
            visible = isIncomingRinging,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            IncomingCallScreen(callManager = appContainer.callManager)
        }

        // Overlay: Active Call Screen
        AnimatedVisibility(
            visible = isActiveCallVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            ActiveCallScreen(callManager = appContainer.callManager)
        }
    }
}
