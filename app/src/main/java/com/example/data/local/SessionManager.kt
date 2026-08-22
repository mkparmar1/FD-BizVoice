package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bizvoice_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ASSIGNED_PHONE = "assigned_phone"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_COMPANY = "user_company"
        private const val KEY_BASE_API_URL = "base_api_url"
        private const val KEY_USE_MOCK_BACKEND = "use_mock_backend"
        private const val KEY_DEFAULT_SPEAKER = "default_speaker"
        private const val KEY_NOISE_CANCELLATION = "noise_cancellation"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEVICE_TOKEN = "device_push_token"
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val DEFAULT_API_URL = "https://api.bizvoice-calling.example.com/api"
    }

    private val _authStateFlow = MutableStateFlow(isLoggedIn())
    val authStateFlow: StateFlow<Boolean> = _authStateFlow.asStateFlow()

    private val _currentUserFlow = MutableStateFlow(getCurrentUser())
    val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    fun saveAuth(token: String, user: User) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_ASSIGNED_PHONE, user.assignedPhoneNumber)
            .putString(KEY_USER_STATUS, user.status)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_USER_COMPANY, user.company)
            .apply()

        _authStateFlow.value = true
        _currentUserFlow.value = user
    }

    fun updateAssignedNumber(phoneNumber: String?) {
        prefs.edit().putString(KEY_ASSIGNED_PHONE, phoneNumber).apply()
        val current = getCurrentUser()
        if (current != null) {
            val updated = current.copy(assignedPhoneNumber = phoneNumber)
            _currentUserFlow.value = updated
        }
    }

    fun updateUserProfile(name: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        val current = getCurrentUser()
        if (current != null) {
            val updated = current.copy(name = name, email = email)
            _currentUserFlow.value = updated
        }
    }

    fun updateFullProfile(user: User) {
        prefs.edit()
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_ASSIGNED_PHONE, user.assignedPhoneNumber)
            .putString(KEY_USER_STATUS, user.status)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_USER_COMPANY, user.company)
            .apply()
        _currentUserFlow.value = user
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    fun getCurrentUser(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val phone = prefs.getString(KEY_ASSIGNED_PHONE, null)
        val status = prefs.getString(KEY_USER_STATUS, "active") ?: "active"
        val role = prefs.getString(KEY_USER_ROLE, "Agent") ?: "Agent"
        val company = prefs.getString(KEY_USER_COMPANY, "BizVoice Global Corp") ?: "BizVoice Global Corp"

        return User(
            id = id,
            name = name,
            email = email,
            assignedPhoneNumber = phone,
            status = status,
            role = role,
            company = company
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_ASSIGNED_PHONE)
            .remove(KEY_USER_STATUS)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_COMPANY)
            .apply()

        _authStateFlow.value = false
        _currentUserFlow.value = null
    }

    // Settings
    var baseApiUrl: String
        get() = prefs.getString(KEY_BASE_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        set(value) = prefs.edit().putString(KEY_BASE_API_URL, value).apply()

    var useMockBackend: Boolean
        get() = prefs.getBoolean(KEY_USE_MOCK_BACKEND, true) // True by default for safe out-of-the-box demoing without breaking
        set(value) = prefs.edit().putBoolean(KEY_USE_MOCK_BACKEND, value).apply()

    var defaultSpeaker: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_SPEAKER, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_SPEAKER, value).apply()

    var noiseCancellation: Boolean
        get() = prefs.getBoolean(KEY_NOISE_CANCELLATION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOISE_CANCELLATION, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var devicePushToken: String
        get() = prefs.getString(KEY_DEVICE_TOKEN, "fcm_token_bizvoice_" + System.currentTimeMillis()) ?: "fcm_token_sample"
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }
}
