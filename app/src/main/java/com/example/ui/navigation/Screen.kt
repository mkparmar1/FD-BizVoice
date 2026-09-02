package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object Main : Screen("main")
    object CallDetail : Screen("call_detail/{callId}") {
        fun createRoute(callId: String) = "call_detail/$callId"
    }
    object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(contactId: String) = "contact_detail/$contactId"
    }
    object AddContact : Screen("contact_add")
    object EditContact : Screen("contact_edit/{contactId}") {
        fun createRoute(contactId: String) = "contact_edit/$contactId"
    }
    object Profile : Screen("profile")
    object Permissions : Screen("permissions")
    object About : Screen("about")
    object BackendConfig : Screen("backend_config")

    // Admin Console Screens
    object AdminConsole : Screen("admin_console")
    object AdminUsers : Screen("admin_users")
    object AdminUserDetail : Screen("admin_user_detail/{userId}") {
        fun createRoute(userId: String) = "admin_user_detail/$userId"
    }
    object AdminNumbers : Screen("admin_numbers")
    object AdminAnalytics : Screen("admin_analytics")
    object AdminFeedback : Screen("admin_feedback")
    object AdminContacts : Screen("admin_contacts")
}

enum class MainTab(val title: String) {
    DIALER("Keypad"),
    RECENTS("Recents"),
    CONTACTS("Contacts"),
    SETTINGS("Settings")
}
