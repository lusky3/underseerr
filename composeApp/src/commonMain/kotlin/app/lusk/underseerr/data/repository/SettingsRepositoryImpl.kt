package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.repository.NotificationSettings
import app.lusk.underseerr.domain.repository.ServerConfig
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of SettingsRepository using DataStore.
 * Feature: underseerr
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
class SettingsRepositoryImpl(
    private val preferencesManager: PreferencesManager,
    private val settingsKtorService: app.lusk.underseerr.data.remote.api.SettingsKtorService,
    private val userKtorService: app.lusk.underseerr.data.remote.api.UserKtorService,
    private val authRepository: app.lusk.underseerr.domain.repository.AuthRepository
) : SettingsRepository {
    
    override fun getThemePreference(): Flow<ThemePreference> =
        preferencesManager.getThemePreference()
    
    override suspend fun setThemePreference(theme: ThemePreference) {
        preferencesManager.setThemePreference(theme)
    }
    
    override fun getNotificationSettings(): Flow<NotificationSettings> =
        preferencesManager.getNotificationSettings()
    
    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        // 1. Update local immediately
        preferencesManager.setNotificationSettings(settings)

        // 2. Sync to Overseerr
        try {
            val userResult = authRepository.getCurrentUser()
            if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
                val userId = userResult.data.id
                
                // Fetch current server state
                val currentApiSettings = userKtorService.getUserNotificationSettings(userId)
                
                // Calculate masks
                val newLocalBitmask = app.lusk.underseerr.data.mapper.NotificationSettingsMapper.calculateBitmask(settings)
                val currentServerBitmask = currentApiSettings.notificationTypes.webpush ?: 0
                
                // Determine target mask based on sync setting
                val finalMask = if (settings.syncEnabled) {
                    // Sync Enabled: Local becomes truth
                    newLocalBitmask
                } else {
                    // Sync Disabled: 
                    // - Turning OFF locally does NOT turn off server (filtering happens on device)
                    // - Turning ON locally MUST turn on server (to ensure delivery)
                    // Logic: Server Mask | Local Mask
                    // Use bitwise OR to ensure everything enabled locally is enabled on server, 
                    // while keeping things enabled on server that might be disabled locally.
                    currentServerBitmask or newLocalBitmask
                }
                
                // Only send update if the mask or enabled status has changed
                if (finalMask != currentServerBitmask || settings.enabled != (currentApiSettings.webPushEnabled == true)) {
                    val updatedApiSettings = currentApiSettings.copy(
                        webPushEnabled = if (settings.syncEnabled) settings.enabled else true, // Force enabled if not syncing to ensure delivery
                        notificationTypes = currentApiSettings.notificationTypes.copy(
                            webpush = finalMask
                        )
                    )
                    userKtorService.updateUserNotificationSettings(userId, updatedApiSettings)
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace() 
        }
    }
    
    override fun getDefaultQualityProfile(): Flow<Int?> =
        preferencesManager.getDefaultQualityProfile()
    
    override suspend fun setDefaultQualityProfile(profileId: Int) {
        preferencesManager.setDefaultQualityProfile(profileId)
    }

    override fun getDefaultMovieQualityProfile(): Flow<Int?> =
        preferencesManager.getDefaultMovieQualityProfile()

    override suspend fun setDefaultMovieQualityProfile(profileId: Int?) {
        preferencesManager.setDefaultMovieQualityProfile(profileId)
    }

    override fun getDefaultTvQualityProfile(): Flow<Int?> =
        preferencesManager.getDefaultTvQualityProfile()

    override suspend fun setDefaultTvQualityProfile(profileId: Int?) {
        preferencesManager.setDefaultTvQualityProfile(profileId)
    }
    
    override fun getBiometricEnabled(): Flow<Boolean> =
        preferencesManager.getBiometricEnabled()
    
    override suspend fun setBiometricEnabled(enabled: Boolean) {
        preferencesManager.setBiometricEnabled(enabled)
    }
    
    override fun getCurrentServerUrl(): Flow<String?> =
        preferencesManager.getCurrentServerUrl()
    
    override suspend fun setCurrentServerUrl(url: String) {
        preferencesManager.setCurrentServerUrl(url)
    }
    
    override fun getConfiguredServers(): Flow<List<ServerConfig>> =
        preferencesManager.getConfiguredServers()
    
    override suspend fun addServer(config: ServerConfig) {
        preferencesManager.addServer(config)
    }
    
    override suspend fun removeServer(url: String) {
        preferencesManager.removeServer(url)
    }

    override fun hasRequestedNotificationPermission(): Flow<Boolean> =
        preferencesManager.hasRequestedNotificationPermission()

    override suspend fun setHasRequestedNotificationPermission(hasRequested: Boolean) {
        preferencesManager.setHasRequestedNotificationPermission(hasRequested)
    }

    override suspend fun getGlobalNotificationSettings(): app.lusk.underseerr.domain.model.Result<Boolean> {
        return app.lusk.underseerr.data.remote.safeApiCall {
            val settings = settingsKtorService.getGlobalWebPushSettings()
            settings.enabled
        }
    }

    override fun getPushToken(): Flow<String?> = preferencesManager.getPushToken()

    override suspend fun savePushToken(token: String) {
        preferencesManager.setPushToken(token)
    }

    override fun getNotificationServerUrl(): Flow<String?> =
        preferencesManager.getNotificationServerUrl()

    override suspend fun setNotificationServerUrl(url: String) {
        preferencesManager.setNotificationServerUrl(url)
    }

    override fun getVibrantThemeColors(): Flow<app.lusk.underseerr.domain.repository.VibrantThemeColors> =
        preferencesManager.getVibrantThemeColors()

    override suspend fun updateVibrantThemeColors(colors: app.lusk.underseerr.domain.repository.VibrantThemeColors) {
        preferencesManager.setVibrantThemeColors(colors)
    }

    override fun getNotificationServerType(): Flow<String> =
        preferencesManager.getNotificationServerType()

    override suspend fun setNotificationServerType(type: String) {
        preferencesManager.setNotificationServerType(type)
    }

    override fun getTrialStartDate(): Flow<Long?> =
        preferencesManager.getTrialStartDate()

    override suspend fun setTrialStartDate(date: Long?) {
        preferencesManager.setTrialStartDate(date)
    }

    override fun getWebhookSecret(): Flow<String?> =
        preferencesManager.getWebhookSecret()

    override suspend fun updateWebhookSecret(secret: String?) {
        preferencesManager.setWebhookSecret(secret)
    }

    override fun getHomeScreenConfig(): Flow<app.lusk.underseerr.domain.repository.HomeScreenConfig> =
        preferencesManager.getHomeScreenConfig()

    override suspend fun updateHomeScreenConfig(config: app.lusk.underseerr.domain.repository.HomeScreenConfig) {
        preferencesManager.setHomeScreenConfig(config)
    }
}
