package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bizvoice_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_AUTH_KEY = "device_auth_key"
        private const val KEY_SECRET_KEY = "secret_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ASSIGNED_PHONE = "assigned_phone"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_COMPANY = "user_company"
        private const val KEY_USER_CREDITS = "user_credits"
        private const val KEY_BASE_API_URL = "base_api_url"
        private const val KEY_USE_MOCK_BACKEND = "use_mock_backend"
        private const val KEY_DEFAULT_SPEAKER = "default_speaker"
        private const val KEY_NOISE_CANCELLATION = "noise_cancellation"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEVICE_TOKEN = "device_push_token"
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"
        private const val KEY_TWILIO_VOICE_TOKEN = "twilio_voice_token"
        private const val KEY_SAVED_LOGIN_EMAIL = "saved_login_email"
        private const val KEY_SAVED_LOGIN_PASSWORD = "saved_login_password"
        private const val KEY_SELECTED_DIALER_COUNTRY_ISO = "selected_dialer_country_iso"
        private const val KEY_CACHED_DIALING_COUNTRIES = "cached_dialing_countries_json"
        private const val KEY_CACHED_DIALING_COUNTRIES_TS = "cached_dialing_countries_ts"

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val DEFAULT_API_URL = "https://api.growfone.com/api/v1.0"
    }

    private val _authStateFlow = MutableStateFlow(isLoggedIn())
    val authStateFlow: StateFlow<Boolean> = _authStateFlow.asStateFlow()

    private val _unauthorizedEventFlow = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val unauthorizedEventFlow: SharedFlow<String?> = _unauthorizedEventFlow.asSharedFlow()

    private val _currentUserFlow = MutableStateFlow(getCurrentUser())
    val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    init {
        // Production Mode: permanently purge and disable any mock backend flag
        prefs.edit().remove(KEY_USE_MOCK_BACKEND).apply()
    }

    fun saveAuth(
        token: String,
        user: User,
        twilioToken: String? = null,
        deviceAuthKey: String? = null
    ) {
        val cleanPhone = user.assignedPhoneNumber?.trim()?.ifBlank { null } ?: "+12183061691"
        val sanitizedUser = user.copy(assignedPhoneNumber = cleanPhone)

        val trimmedToken = token.trim()
        val effectiveAuthKey = when {
            !deviceAuthKey.isNullOrBlank() -> deviceAuthKey.trim()
            !user.authKey.isNullOrBlank() -> user.authKey!!.trim()
            else -> trimmedToken
        }

        val editor = prefs.edit()
            .putString(KEY_AUTH_TOKEN, trimmedToken)
            .putString(KEY_DEVICE_AUTH_KEY, effectiveAuthKey)
            .putString(KEY_USER_ID, sanitizedUser.id)
            .putString(KEY_USER_NAME, sanitizedUser.name)
            .putString(KEY_USER_EMAIL, sanitizedUser.email)
            .putString(KEY_ASSIGNED_PHONE, cleanPhone)
            .putString(KEY_USER_STATUS, sanitizedUser.status)
            .putString(KEY_USER_ROLE, sanitizedUser.role)
            .putString(KEY_USER_COMPANY, sanitizedUser.company)
            .putInt(KEY_USER_CREDITS, sanitizedUser.credits)

        if (twilioToken != null) {
            editor.putString(KEY_TWILIO_VOICE_TOKEN, twilioToken)
        }
        editor.apply()

        _authStateFlow.value = true
        _currentUserFlow.value = sanitizedUser.copy(authKey = effectiveAuthKey)
    }

    fun saveTwilioVoiceToken(token: String?) {
        prefs.edit().putString(KEY_TWILIO_VOICE_TOKEN, token).apply()
    }

    fun getTwilioVoiceToken(): String? {
        return prefs.getString(KEY_TWILIO_VOICE_TOKEN, null)
    }

    fun updateAssignedNumber(phoneNumber: String?) {
        prefs.edit().putString(KEY_ASSIGNED_PHONE, phoneNumber).apply()
        val current = getCurrentUser()
        if (current != null) {
            val updated = current.copy(assignedPhoneNumber = phoneNumber)
            _currentUserFlow.value = updated
        }
    }

    fun updateCredits(credits: Int) {
        prefs.edit().putInt(KEY_USER_CREDITS, credits).apply()
        val current = getCurrentUser()
        if (current != null) {
            val updated = current.copy(credits = credits)
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
        val editor = prefs.edit()
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_ASSIGNED_PHONE, user.assignedPhoneNumber)
            .putString(KEY_USER_STATUS, user.status)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_USER_COMPANY, user.company)
            .putInt(KEY_USER_CREDITS, user.credits)

        if (!user.authKey.isNullOrBlank()) {
            editor.putString(KEY_DEVICE_AUTH_KEY, user.authKey.trim())
        }
        editor.apply()
        _currentUserFlow.value = user
    }

    fun getAuthToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)?.trim()
        return if (token.isNullOrBlank()) null else token
    }

    fun getDeviceAuthKey(): String {
        val stored = prefs.getString(KEY_DEVICE_AUTH_KEY, null)?.trim()
        if (!stored.isNullOrBlank() && stored != "device_auth_key_default") {
            return stored
        }
        val token = getAuthToken()
        if (!token.isNullOrBlank()) {
            return token
        }
        val userAuthKey = getCurrentUser()?.authKey?.trim()
        if (!userAuthKey.isNullOrBlank()) {
            return userAuthKey
        }
        return ""
    }

    fun getEffectiveAuthToken(): String {
        val token = getAuthToken()
        if (!token.isNullOrBlank()) return token
        val deviceAuth = getDeviceAuthKey()
        if (deviceAuth.isNotBlank() && deviceAuth != "device_auth_key_default") return deviceAuth
        return ""
    }

    fun setDeviceAuthKey(key: String) {
        prefs.edit().putString(KEY_DEVICE_AUTH_KEY, key.trim()).apply()
    }

    fun getSecretKey(): String {
        return prefs.getString(KEY_SECRET_KEY, "growfone_secret_key_default") ?: "growfone_secret_key_default"
    }

    fun setSecretKey(key: String) {
        prefs.edit().putString(KEY_SECRET_KEY, key).apply()
    }

    fun isLoggedIn(): Boolean {
        return getEffectiveAuthToken().isNotBlank()
    }

    fun getCurrentUser(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val phone = prefs.getString(KEY_ASSIGNED_PHONE, null)?.ifBlank { null } ?: "+12183061691"
        val status = prefs.getString(KEY_USER_STATUS, "active") ?: "active"
        val role = prefs.getString(KEY_USER_ROLE, "user") ?: "user"
        val company = prefs.getString(KEY_USER_COMPANY, "BizVoice Global Corp") ?: "BizVoice Global Corp"
        val credits = prefs.getInt(KEY_USER_CREDITS, 250)
        val authKey = prefs.getString(KEY_DEVICE_AUTH_KEY, null)?.ifBlank { null } ?: getAuthToken()

        return User(
            id = id,
            name = name,
            email = email,
            assignedPhoneNumber = phone,
            status = status,
            role = role,
            company = company,
            credits = credits,
            authKey = authKey
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_DEVICE_AUTH_KEY)
            .remove(KEY_TWILIO_VOICE_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_ASSIGNED_PHONE)
            .remove(KEY_USER_STATUS)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_COMPANY)
            .remove(KEY_USER_CREDITS)
            .apply()

        _authStateFlow.value = false
        _currentUserFlow.value = null
    }

    fun notifyUnauthorized(reason: String = "Session expired. Please log in again.") {
        clearSession()
        _unauthorizedEventFlow.tryEmit(reason)
    }

    // Settings
    var baseApiUrl: String
        get() = prefs.getString(KEY_BASE_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        set(value) = prefs.edit().putString(KEY_BASE_API_URL, value).apply()

    val useMockBackend: Boolean
        get() = false

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
        get() = prefs.getString(KEY_DEVICE_TOKEN, "fcm_token_growfone_" + System.currentTimeMillis()) ?: "fcm_token_sample"
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    fun saveLastLoginCredentials(email: String, pass: String) {
        prefs.edit()
            .putString(KEY_SAVED_LOGIN_EMAIL, email)
            .putString(KEY_SAVED_LOGIN_PASSWORD, pass)
            .apply()
    }

    fun getSavedLoginEmail(): String {
        return prefs.getString(KEY_SAVED_LOGIN_EMAIL, "") ?: ""
    }

    fun getSavedLoginPassword(): String {
        return prefs.getString(KEY_SAVED_LOGIN_PASSWORD, "") ?: ""
    }

    var selectedDialerCountryIso: String?
        get() = prefs.getString(KEY_SELECTED_DIALER_COUNTRY_ISO, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_DIALER_COUNTRY_ISO, value).apply()

    var cachedDialingCountriesJson: String?
        get() = prefs.getString(KEY_CACHED_DIALING_COUNTRIES, null)
        set(value) = prefs.edit().putString(KEY_CACHED_DIALING_COUNTRIES, value).apply()

    var cachedDialingCountriesTimestamp: Long
        get() = prefs.getLong(KEY_CACHED_DIALING_COUNTRIES_TS, 0L)
        set(value) = prefs.edit().putLong(KEY_CACHED_DIALING_COUNTRIES_TS, value).apply()
}
