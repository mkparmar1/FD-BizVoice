package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.contacts.AddEditContactScreen
import com.example.ui.screens.contacts.ContactDetailScreen
import com.example.ui.screens.main.MainContainerScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.recents.CallDetailScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.BackendConfigScreen
import com.example.ui.screens.settings.PermissionsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.BizVoiceTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: BizVoiceAppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = BizVoiceAppContainer(applicationContext)
        enableEdgeToEdge()

        setContent {
            val themeMode by appContainer.sessionManager.themeModeFlow.collectAsState(initial = appContainer.sessionManager.themeMode)

            BizVoiceTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BizVoiceNavigation(appContainer = appContainer)
                }
            }
        }
    }
}

@Composable
fun BizVoiceNavigation(appContainer: BizVoiceAppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Request all required runtime permissions upfront when the app is installed and opened
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val ungranted = requiredPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionsLauncher.launch(ungranted.toTypedArray())
        }
    }

    // Automatically navigate to Login when 401 Unauthorized occurs
    LaunchedEffect(Unit) {
        appContainer.sessionManager.unauthorizedEventFlow.collect {
            try {
                appContainer.callManager.endCall()
            } catch (_: Exception) {}
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                sessionManager = appContainer.sessionManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                repository = appContainer.repository,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToBackendConfig = {
                    navController.navigate(Screen.BackendConfig.route)
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen(
                appContainer = appContainer,
                onNavigateToCallDetail = { callId ->
                    navController.navigate(Screen.CallDetail.createRoute(callId))
                },
                onNavigateToContactDetail = { contactId ->
                    navController.navigate(Screen.ContactDetail.createRoute(contactId))
                },
                onNavigateToAddContact = {
                    navController.navigate(Screen.AddContact.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                },
                onNavigateToBackendConfig = {
                    navController.navigate(Screen.BackendConfig.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CallDetail.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            CallDetailScreen(
                callId = callId,
                repository = appContainer.repository,
                callManager = appContainer.callManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddContact = { phone ->
                    navController.navigate("${Screen.AddContact.route}?phone=$phone")
                },
                onNavigateToContactDetail = { contactId ->
                    navController.navigate(Screen.ContactDetail.createRoute(contactId))
                }
            )
        }

        composable(
            route = "${Screen.AddContact.route}?phone={phone}",
            arguments = listOf(navArgument("phone") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone")
            AddEditContactScreen(
                initialPhoneNumber = phone,
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddContact.route) {
            AddEditContactScreen(
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ContactDetail.route,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                contactId = contactId,
                repository = appContainer.repository,
                callManager = appContainer.callManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditContact.createRoute(id))
                },
                onNavigateToCallDetail = { callId ->
                    navController.navigate(Screen.CallDetail.createRoute(callId))
                }
            )
        }

        composable(
            route = Screen.EditContact.route,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            AddEditContactScreen(
                contactId = contactId,
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() },
                onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BackendConfig.route) {
            BackendConfigScreen(
                sessionManager = appContainer.sessionManager,
                repository = appContainer.repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
