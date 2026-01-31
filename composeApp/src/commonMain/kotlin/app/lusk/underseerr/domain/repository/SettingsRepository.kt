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
}

/**
 * Theme preference options.
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
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
