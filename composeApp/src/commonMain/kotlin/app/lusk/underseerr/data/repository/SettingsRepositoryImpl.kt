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
    private val preferencesManager: PreferencesManager
) : SettingsRepository {
    
    override fun getThemePreference(): Flow<ThemePreference> =
        preferencesManager.getThemePreference()
    
    override suspend fun setThemePreference(theme: ThemePreference) {
        preferencesManager.setThemePreference(theme)
    }
    
    override fun getNotificationSettings(): Flow<NotificationSettings> =
        preferencesManager.getNotificationSettings()
    
    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        preferencesManager.setNotificationSettings(settings)
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
}
