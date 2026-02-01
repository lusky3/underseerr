package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Repository interface for app settings operations.
 * Feature: underseerr
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
interface SettingsRepository {
    
    /**
     * Get theme preference.
     * Property 20: Theme Application
     */
    fun getThemePreference(): Flow<ThemePreference>
    
    /**
     * Set theme preference.
     * Property 20: Theme Application
     */
    suspend fun setThemePreference(theme: ThemePreference)
    
    /**
     * Get notification settings.
     * Property 22: Notification Permission Flow
     */
    fun getNotificationSettings(): Flow<NotificationSettings>
    
    /**
     * Update notification settings.
     * Property 22: Notification Permission Flow
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings)
    
    /**
     * Get default quality profile.
     * @deprecated Use getDefaultMovieQualityProfile or getDefaultTvQualityProfile
     */
    fun getDefaultQualityProfile(): Flow<Int?>
    
    /**
     * Set default quality profile.
     * @deprecated Use setDefaultMovieQualityProfile or setDefaultTvQualityProfile
     */
    suspend fun setDefaultQualityProfile(profileId: Int)

    /**
     * Get default movie quality profile.
     */
    fun getDefaultMovieQualityProfile(): Flow<Int?>

    /**
     * Set default movie quality profile.
     */
    suspend fun setDefaultMovieQualityProfile(profileId: Int?)

    /**
     * Get default TV quality profile.
     */
    fun getDefaultTvQualityProfile(): Flow<Int?>

    /**
     * Set default TV quality profile.
     */
    suspend fun setDefaultTvQualityProfile(profileId: Int?)
    
    /**
     * Get biometric authentication enabled status.
     */
    fun getBiometricEnabled(): Flow<Boolean>
    
    /**
     * Set biometric authentication enabled status.
     */
    suspend fun setBiometricEnabled(enabled: Boolean)
    
    /**
     * Get current server URL.
     * Property 23: Multi-Server Switching
     */
    fun getCurrentServerUrl(): Flow<String?>
    
    /**
     * Set current server URL.
     * Property 23: Multi-Server Switching
     */
    suspend fun setCurrentServerUrl(url: String)
    
    /**
     * Get all configured servers.
     * Property 23: Multi-Server Switching
     */
    fun getConfiguredServers(): Flow<List<ServerConfig>>
    
    /**
     * Add a new server configuration.
     * Property 23: Multi-Server Switching
     */
    suspend fun addServer(config: ServerConfig)
    
    /**
     * Remove a server configuration.
     * Property 23: Multi-Server Switching
     */
    suspend fun removeServer(url: String)
    
    fun hasRequestedNotificationPermission(): Flow<Boolean>
    suspend fun setHasRequestedNotificationPermission(hasRequested: Boolean)

    /**
     * Get global notification settings from the server.
     */
    /**
     * Get global notification settings from the server.
     */
    suspend fun getGlobalNotificationSettings(): Result<Boolean>

    /**
     * Get cached FCM push token.
     */
    fun getPushToken(): Flow<String?>

    /**
     * Save FCM push token.
     */
    suspend fun savePushToken(token: String)

    /**
     * Get configured Notification Server URL (Cloudflare Worker).
     */
    fun getNotificationServerUrl(): Flow<String?>

    /**
     * Set Notification Server URL.
     */
    suspend fun setNotificationServerUrl(url: String)

    /**
     * Get custom vibrant theme colors.
     */
    fun getVibrantThemeColors(): Flow<VibrantThemeColors>

    /**
     * Update custom vibrant theme colors.
     */
    suspend fun updateVibrantThemeColors(colors: VibrantThemeColors)

    /**
     * Get the type of notification server being used.
     */
    fun getNotificationServerType(): Flow<String>

    /**
     * Set the type of notification server.
     */
    suspend fun setNotificationServerType(type: String)

    /**
     * Get the trial start date in millis.
     */
    fun getTrialStartDate(): Flow<Long?>

    /**
     * Set the trial start date.
     */
    suspend fun setTrialStartDate(date: Long?)
}

/**
 * Theme preference options.
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
    VIBRANT
}

/**
 * Notification settings.
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val requestPendingApproval: Boolean = true,
    val requestApproved: Boolean = true,
    val requestAutoApproved: Boolean = true,
    val requestDeclined: Boolean = true,
    val requestProcessingFailed: Boolean = true,
    val requestAvailable: Boolean = true,
    val issueReported: Boolean = true,
    val issueComment: Boolean = true,
    val issueResolved: Boolean = true,
    val issueReopened: Boolean = true,
    val mediaAutoRequested: Boolean = true,
    val syncEnabled: Boolean = true
)

/**
 * Server configuration.
 */
@Serializable
data class ServerConfig(
    val url: String,
    val name: String,
    val isActive: Boolean = false
)

/**
 * Custom colors for the Vibrant theme.
 * Hex strings in ARGB format (e.g., #FFFFFFFF)
 */
@Serializable
data class VibrantThemeColors(
    val primaryStart: String = "#FF5D63EA",
    val primaryEnd: String = "#FF7A31AC",
    val secondaryStart: String = "#FF0FEE8D",
    val secondaryEnd: String = "#FF09AA9D",
    val tertiaryStart: String = "#FFFF9800",
    val tertiaryEnd: String = "#FFFF5722",
    val backgroundStart: String = "#FF0A001F",
    val backgroundEnd: String = "#FF1A0033",
    val surfaceStart: String = "#FF1A0033",
    val surfaceEnd: String = "#FF2D0055",
    val accentStart: String = "#FF00C6FF",
    val accentEnd: String = "#FF0072FF",
    val highlightStart: String = "#FFFF4B2B",
    val highlightEnd: String = "#FFFF416C",
    val appBarStart: String = "#FF5D63EA",
    val appBarEnd: String = "#FF7A31AC",
    val navBarStart: String = "#FF0A001F",
    val navBarEnd: String = "#FF1A0033",
    val settingsStart: String = "#FF0A001F",
    val settingsEnd: String = "#FF1A0033",
    val profilesStart: String = "#FF0A001F",
    val profilesEnd: String = "#FF1A0033",
    val requestDetailsStart: String = "#FF0A001F",
    val requestDetailsEnd: String = "#FF1A0033",
    val issueDetailsStart: String = "#FF0A001F",
    val issueDetailsEnd: String = "#FF1A0033",
    val usePillShape: Boolean = true
)
