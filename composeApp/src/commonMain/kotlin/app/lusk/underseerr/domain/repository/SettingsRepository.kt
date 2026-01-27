package app.lusk.underseerr.domain.repository

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
    val requestApproved: Boolean = true,
    val requestAvailable: Boolean = true,
    val requestDeclined: Boolean = true
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
