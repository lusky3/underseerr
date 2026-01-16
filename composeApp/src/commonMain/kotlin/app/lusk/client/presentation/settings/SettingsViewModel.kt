package app.lusk.client.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.repository.NotificationSettings
import app.lusk.client.domain.repository.ServerConfig
import app.lusk.client.domain.repository.SettingsRepository
import app.lusk.client.domain.repository.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for settings screen.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
/**
 * ViewModel for settings screen.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: app.lusk.client.domain.repository.AuthRepository
) : ViewModel() {
    
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    
    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()
    
    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()
    
    private val _defaultQualityProfile = MutableStateFlow<Int?>(null)
    val defaultQualityProfile: StateFlow<Int?> = _defaultQualityProfile.asStateFlow()
    
    private val _configuredServers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val configuredServers: StateFlow<List<ServerConfig>> = _configuredServers.asStateFlow()
    
    private val _currentServerUrl = MutableStateFlow<String?>(null)
    val currentServerUrl: StateFlow<String?> = _currentServerUrl.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getThemePreference().collect {
                _themePreference.value = it
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getNotificationSettings().collect {
                _notificationSettings.value = it
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getBiometricEnabled().collect {
                _biometricEnabled.value = it
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getDefaultQualityProfile().collect {
                _defaultQualityProfile.value = it
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getConfiguredServers().collect {
                _configuredServers.value = it
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getCurrentServerUrl().collect {
                _currentServerUrl.value = it
            }
        }
    }
    
    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
        }
    }
    
    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            settingsRepository.updateNotificationSettings(settings)
        }
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }
    
    fun setDefaultQualityProfile(profileId: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultQualityProfile(profileId)
        }
    }
    
    fun switchServer(url: String) {
        viewModelScope.launch {
            authRepository.logout()
            settingsRepository.setCurrentServerUrl(url)
        }
    }
    
    fun addServer(config: ServerConfig) {
        viewModelScope.launch {
            settingsRepository.addServer(config)
        }
    }
    
    fun removeServer(url: String) {
        viewModelScope.launch {
            settingsRepository.removeServer(url)
        }
    }
}
