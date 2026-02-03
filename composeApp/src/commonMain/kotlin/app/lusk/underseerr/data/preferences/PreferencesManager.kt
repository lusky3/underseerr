package app.lusk.underseerr.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import app.lusk.underseerr.domain.repository.NotificationSettings
import app.lusk.underseerr.domain.repository.ServerConfig
import app.lusk.underseerr.domain.repository.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages application preferences using DataStore.
 * Feature: underseerr
 * Validates: Requirements 5.2, 5.3
 */
class PreferencesManager(
    private val dataStore: DataStore<Preferences>
) {
    
    /**
     * Preference keys.
     */
    object PreferenceKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val USER_ID = intPreferencesKey("user_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_APPROVED = booleanPreferencesKey("notification_approved")
        val NOTIFICATION_AVAILABLE = booleanPreferencesKey("notification_available")
        val NOTIFICATION_DECLINED = booleanPreferencesKey("notification_declined")
        val NOTIFICATION_PENDING = booleanPreferencesKey("notification_pending")
        val NOTIFICATION_AUTO_APPROVED = booleanPreferencesKey("notification_auto_approved")
        val NOTIFICATION_FAILED = booleanPreferencesKey("notification_failed")
        val NOTIFICATION_ISSUE_REPORTED = booleanPreferencesKey("notification_issue_reported")
        val NOTIFICATION_ISSUE_COMMENT = booleanPreferencesKey("notification_issue_comment")
        val NOTIFICATION_ISSUE_RESOLVED = booleanPreferencesKey("notification_issue_resolved")
        val NOTIFICATION_ISSUE_REOPENED = booleanPreferencesKey("notification_issue_reopened")
        val SYNC_NOTIFICATION_SETTINGS = booleanPreferencesKey("sync_notification_settings")
        val DEFAULT_QUALITY_PROFILE = intPreferencesKey("default_quality_profile")
        val DEFAULT_MOVIE_QUALITY_PROFILE = intPreferencesKey("default_movie_quality_profile")
        val DEFAULT_TV_QUALITY_PROFILE = intPreferencesKey("default_tv_quality_profile")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CONFIGURED_SERVERS = stringPreferencesKey("configured_servers")
        val CURRENT_SERVER_URL = stringPreferencesKey("current_server_url")
        val CLIENT_ID = stringPreferencesKey("client_id")
        val HAS_REQUESTED_NOTIFICATIONS = booleanPreferencesKey("has_requested_notifications")
        val PUSH_TOKEN = stringPreferencesKey("push_token")
        val NOTIFICATION_SERVER_URL = stringPreferencesKey("notification_server_url")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val VIBRANT_THEME_COLORS = stringPreferencesKey("vibrant_theme_colors")
        val TRIAL_START_DATE = longPreferencesKey("trial_start_date")
        val NOTIFICATION_SERVER_TYPE = stringPreferencesKey("notification_server_type")
        val PREMIUM_EXPIRES_AT = longPreferencesKey("premium_expires_at")
        val WEBHOOK_SECRET = stringPreferencesKey("webhook_secret")
        val HOME_SCREEN_CONFIG = stringPreferencesKey("home_screen_config")
    }
    
    /**
     * Theme modes.
     */
    enum class ThemeMode {
        LIGHT,
        DARK,
        SYSTEM,
        VIBRANT
    }
    
    // Server Configuration
    
    suspend fun setServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SERVER_URL] = url
        }
    }
    
    fun getServerUrl(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.SERVER_URL]
        }
    }
    
    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
        }
    }
    
    fun getApiKey(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.API_KEY]
        }
    }
    
    suspend fun setUserId(userId: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_ID] = userId
        }
    }
    
    fun getUserId(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.USER_ID]
        }
    }
    
    // Theme Settings
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode.name
        }
    }
    
    fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data.map { preferences ->
            val modeName = preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }
    }
    
    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }
    
    fun getDynamicColorEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.DYNAMIC_COLOR_ENABLED] ?: true
        }
    }
    
    // Security Settings
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BIOMETRIC_ENABLED] = enabled
        }
    }
    
    fun getBiometricEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.BIOMETRIC_ENABLED] ?: false
        }
    }
    
    // Notification Settings
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true
        }
    }
    
    suspend fun setNotificationApprovedEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_APPROVED] = enabled
        }
    }
    
    fun getNotificationApprovedEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_APPROVED] ?: true
        }
    }
    
    suspend fun setNotificationAvailableEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_AVAILABLE] = enabled
        }
    }
    
    fun getNotificationAvailableEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_AVAILABLE] ?: true
        }
    }
    
    suspend fun setNotificationDeclinedEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_DECLINED] = enabled
        }
    }
    
    fun getNotificationDeclinedEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_DECLINED] ?: true
        }
    }
    
    // Media Settings
    
    suspend fun setDefaultQualityProfile(profileId: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_QUALITY_PROFILE] = profileId
        }
    }
    
    fun getDefaultQualityProfile(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.DEFAULT_QUALITY_PROFILE]
        }
    }

    suspend fun setDefaultMovieQualityProfile(profileId: Int?) {
        dataStore.edit { preferences ->
            if (profileId != null) {
                preferences[PreferenceKeys.DEFAULT_MOVIE_QUALITY_PROFILE] = profileId
            } else {
                preferences.remove(PreferenceKeys.DEFAULT_MOVIE_QUALITY_PROFILE)
            }
        }
    }
    
    fun getDefaultMovieQualityProfile(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.DEFAULT_MOVIE_QUALITY_PROFILE]
        }
    }
    
    suspend fun setDefaultTvQualityProfile(profileId: Int?) {
        dataStore.edit { preferences ->
            if (profileId != null) {
                preferences[PreferenceKeys.DEFAULT_TV_QUALITY_PROFILE] = profileId
            } else {
                preferences.remove(PreferenceKeys.DEFAULT_TV_QUALITY_PROFILE)
            }
        }
    }
    
    fun getDefaultTvQualityProfile(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.DEFAULT_TV_QUALITY_PROFILE]
        }
    }
    
    // Onboarding
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    fun getOnboardingCompleted(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
        }
    }
    
    // Clear All
    
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.API_KEY)
            preferences.remove(PreferenceKeys.USER_ID)
            preferences.remove(PreferenceKeys.DEFAULT_QUALITY_PROFILE)
            preferences.remove(PreferenceKeys.DEFAULT_MOVIE_QUALITY_PROFILE)
            preferences.remove(PreferenceKeys.DEFAULT_TV_QUALITY_PROFILE)
        }
    }
    
    // New methods for SettingsRepository
    
    fun getThemePreference(): Flow<ThemePreference> {
        return getThemeMode().map { mode ->
            when (mode) {
                ThemeMode.LIGHT -> ThemePreference.LIGHT
                ThemeMode.DARK -> ThemePreference.DARK
                ThemeMode.SYSTEM -> ThemePreference.SYSTEM
                ThemeMode.VIBRANT -> ThemePreference.VIBRANT
            }
        }
    }
    
    suspend fun setThemePreference(theme: ThemePreference) {
        val mode = when (theme) {
            ThemePreference.LIGHT -> ThemeMode.LIGHT
            ThemePreference.DARK -> ThemeMode.DARK
            ThemePreference.SYSTEM -> ThemeMode.SYSTEM
            ThemePreference.VIBRANT -> ThemeMode.VIBRANT
        }
        setThemeMode(mode)
    }
    
    fun getNotificationSettings(): Flow<NotificationSettings> {
        return dataStore.data.map { preferences ->
            NotificationSettings(
                enabled = preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true,
                requestApproved = preferences[PreferenceKeys.NOTIFICATION_APPROVED] ?: true,
                requestAvailable = preferences[PreferenceKeys.NOTIFICATION_AVAILABLE] ?: true,
                requestDeclined = preferences[PreferenceKeys.NOTIFICATION_DECLINED] ?: true,
                requestPendingApproval = preferences[PreferenceKeys.NOTIFICATION_PENDING] ?: true,
                requestAutoApproved = preferences[PreferenceKeys.NOTIFICATION_AUTO_APPROVED] ?: true,
                requestProcessingFailed = preferences[PreferenceKeys.NOTIFICATION_FAILED] ?: true,
                issueReported = preferences[PreferenceKeys.NOTIFICATION_ISSUE_REPORTED] ?: true,
                issueComment = preferences[PreferenceKeys.NOTIFICATION_ISSUE_COMMENT] ?: true,
                issueResolved = preferences[PreferenceKeys.NOTIFICATION_ISSUE_RESOLVED] ?: true,
                issueReopened = preferences[PreferenceKeys.NOTIFICATION_ISSUE_REOPENED] ?: true,
                syncEnabled = preferences[PreferenceKeys.SYNC_NOTIFICATION_SETTINGS] ?: true
            )
        }
    }
    
    suspend fun setNotificationSettings(settings: NotificationSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = settings.enabled
            preferences[PreferenceKeys.NOTIFICATION_APPROVED] = settings.requestApproved
            preferences[PreferenceKeys.NOTIFICATION_AVAILABLE] = settings.requestAvailable
            preferences[PreferenceKeys.NOTIFICATION_DECLINED] = settings.requestDeclined
            preferences[PreferenceKeys.NOTIFICATION_PENDING] = settings.requestPendingApproval
            preferences[PreferenceKeys.NOTIFICATION_AUTO_APPROVED] = settings.requestAutoApproved
            preferences[PreferenceKeys.NOTIFICATION_FAILED] = settings.requestProcessingFailed
            preferences[PreferenceKeys.NOTIFICATION_ISSUE_REPORTED] = settings.issueReported
            preferences[PreferenceKeys.NOTIFICATION_ISSUE_COMMENT] = settings.issueComment
            preferences[PreferenceKeys.NOTIFICATION_ISSUE_RESOLVED] = settings.issueResolved
            preferences[PreferenceKeys.NOTIFICATION_ISSUE_REOPENED] = settings.issueReopened
            preferences[PreferenceKeys.SYNC_NOTIFICATION_SETTINGS] = settings.syncEnabled
        }
    }
    
    fun getCurrentServerUrl(): Flow<String?> {
        return getServerUrl()
    }
    
    suspend fun setCurrentServerUrl(url: String) {
        setServerUrl(url)
    }
    
    fun getConfiguredServers(): Flow<List<ServerConfig>> {
        return dataStore.data.map { preferences ->
            val serversJson = preferences[PreferenceKeys.CONFIGURED_SERVERS]
            if (serversJson != null) {
                try {
                    Json.decodeFromString<List<ServerConfig>>(serversJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    
    suspend fun addServer(config: ServerConfig) {
        dataStore.edit { preferences ->
            val currentServers = preferences[PreferenceKeys.CONFIGURED_SERVERS]?.let {
                try {
                    Json.decodeFromString<List<ServerConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            
            val updatedServers = currentServers.toMutableList().apply {
                // Remove existing server with same URL
                removeAll { it.url == config.url }
                // Add new config
                add(config)
            }
            
            preferences[PreferenceKeys.CONFIGURED_SERVERS] = Json.encodeToString(updatedServers)
        }
    }
    
    suspend fun removeServer(url: String) {
        dataStore.edit { preferences ->
            val currentServers = preferences[PreferenceKeys.CONFIGURED_SERVERS]?.let {
                try {
                    Json.decodeFromString<List<ServerConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            
            val updatedServers = currentServers.filter { it.url != url }
            
            preferences[PreferenceKeys.CONFIGURED_SERVERS] = Json.encodeToString(updatedServers)
        }
    }
    
    suspend fun setClientId(clientId: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CLIENT_ID] = clientId
        }
    }
    
    suspend fun getClientId(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.CLIENT_ID]
        }.first()
    }

    suspend fun setHasRequestedNotificationPermission(hasRequested: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_REQUESTED_NOTIFICATIONS] = hasRequested
        }
    }

    fun hasRequestedNotificationPermission(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.HAS_REQUESTED_NOTIFICATIONS] ?: false
        }
    }

    suspend fun setPushToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PUSH_TOKEN] = token
        }
    }

    fun getPushToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.PUSH_TOKEN]
        }
    }

    suspend fun setNotificationServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_SERVER_URL] = url
        }
    }

    fun getNotificationServerUrl(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_SERVER_URL]
        }
    }

    suspend fun setIsPremium(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_PREMIUM] = isPremium
        }
    }

    fun getIsPremium(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.IS_PREMIUM] ?: false
        }
    }

    fun getVibrantThemeColors(): Flow<app.lusk.underseerr.domain.repository.VibrantThemeColors> {
        return dataStore.data.map { preferences ->
            val json = preferences[PreferenceKeys.VIBRANT_THEME_COLORS]
            if (json != null) {
                try {
                    Json.decodeFromString<app.lusk.underseerr.domain.repository.VibrantThemeColors>(json)
                } catch (e: Exception) {
                    app.lusk.underseerr.domain.repository.VibrantThemeColors()
                }
            } else {
                app.lusk.underseerr.domain.repository.VibrantThemeColors()
            }
        }
    }

    suspend fun setVibrantThemeColors(colors: app.lusk.underseerr.domain.repository.VibrantThemeColors) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VIBRANT_THEME_COLORS] = Json.encodeToString(colors)
        }
    }

    suspend fun setTrialStartDate(date: Long?) {
        dataStore.edit { preferences ->
            if (date != null) {
                preferences[PreferenceKeys.TRIAL_START_DATE] = date
            } else {
                preferences.remove(PreferenceKeys.TRIAL_START_DATE)
            }
        }
    }

    fun getTrialStartDate(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.TRIAL_START_DATE]
        }
    }

    suspend fun setNotificationServerType(type: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_SERVER_TYPE] = type
        }
    }

    fun getNotificationServerType(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_SERVER_TYPE] ?: "HOSTED"
        }
    }

    suspend fun setPremiumExpiresAt(date: Long?) {
        dataStore.edit { preferences ->
            if (date != null) {
                preferences[PreferenceKeys.PREMIUM_EXPIRES_AT] = date
            } else {
                preferences.remove(PreferenceKeys.PREMIUM_EXPIRES_AT)
            }
        }
    }

    fun getPremiumExpiresAt(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.PREMIUM_EXPIRES_AT]
        }
    }

    suspend fun setWebhookSecret(secret: String?) {
        dataStore.edit { preferences ->
            if (secret != null) {
                preferences[PreferenceKeys.WEBHOOK_SECRET] = secret
            } else {
                preferences.remove(PreferenceKeys.WEBHOOK_SECRET)
            }
        }
    }

    fun getWebhookSecret(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferenceKeys.WEBHOOK_SECRET]
        }
    }

    fun getHomeScreenConfig(): Flow<app.lusk.underseerr.domain.repository.HomeScreenConfig> {
        return dataStore.data.map { preferences ->
            val json = preferences[PreferenceKeys.HOME_SCREEN_CONFIG]
            if (json != null) {
                try {
                    Json.decodeFromString<app.lusk.underseerr.domain.repository.HomeScreenConfig>(json)
                } catch (e: Exception) {
                    app.lusk.underseerr.domain.repository.HomeScreenConfig()
                }
            } else {
                app.lusk.underseerr.domain.repository.HomeScreenConfig()
            }
        }
    }

    suspend fun setHomeScreenConfig(config: app.lusk.underseerr.domain.repository.HomeScreenConfig) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HOME_SCREEN_CONFIG] = Json.encodeToString(config)
        }
    }
}
