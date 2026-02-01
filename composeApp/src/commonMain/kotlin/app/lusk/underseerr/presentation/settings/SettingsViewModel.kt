package app.lusk.underseerr.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.model.AppPermissions
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.NotificationSettings
import app.lusk.underseerr.domain.repository.ServerConfig
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.ThemePreference
import app.lusk.underseerr.domain.model.SubscriptionStatus
import app.lusk.underseerr.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow


/**
 * ViewModel for settings screen.
 * Feature: underseerr
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
/**
 * ViewModel for settings screen.
 * Feature: underseerr
 * Validates: Requirements 5.2, 5.3, 5.5, 5.6
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: app.lusk.underseerr.domain.repository.AuthRepository,
    private val requestRepository: app.lusk.underseerr.domain.repository.RequestRepository,
    private val biometricManager: app.lusk.underseerr.domain.security.BiometricManager,
    private val permissionManager: app.lusk.underseerr.domain.permission.PermissionManager,
    private val notificationRepository: app.lusk.underseerr.domain.repository.NotificationRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    
    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()
    
    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()
    
    private val _defaultMovieProfile = MutableStateFlow<Int?>(null)
    val defaultMovieProfile: StateFlow<Int?> = _defaultMovieProfile.asStateFlow()

    private val _defaultTvProfile = MutableStateFlow<Int?>(null)
    val defaultTvProfile: StateFlow<Int?> = _defaultTvProfile.asStateFlow()

    private val _movieProfiles = MutableStateFlow<List<app.lusk.underseerr.domain.repository.QualityProfile>>(emptyList())
    val movieProfiles: StateFlow<List<app.lusk.underseerr.domain.repository.QualityProfile>> = _movieProfiles.asStateFlow()

    private val _tvProfiles = MutableStateFlow<List<app.lusk.underseerr.domain.repository.QualityProfile>>(emptyList())
    val tvProfiles: StateFlow<List<app.lusk.underseerr.domain.repository.QualityProfile>> = _tvProfiles.asStateFlow()
    
    private val _configuredServers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val configuredServers: StateFlow<List<ServerConfig>> = _configuredServers.asStateFlow()
    
    private val _currentServerUrl = MutableStateFlow<String?>(null)
    val currentServerUrl: StateFlow<String?> = _currentServerUrl.asStateFlow()
    
    private val _globalWebPushEnabled = MutableStateFlow<Boolean>(true)
    val globalWebPushEnabled: StateFlow<Boolean> = _globalWebPushEnabled.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus())
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _vibrantThemeColors = MutableStateFlow(app.lusk.underseerr.domain.repository.VibrantThemeColors())
    val vibrantThemeColors: StateFlow<app.lusk.underseerr.domain.repository.VibrantThemeColors> = _vibrantThemeColors.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent
    
    init {
        loadSettings()
        fetchProfiles()
        checkBiometricAvailability()
    }
    
    private fun checkBiometricAvailability() {
        _isBiometricAvailable.value = biometricManager.isBiometricAvailable()
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
            settingsRepository.getDefaultMovieQualityProfile().collect {
                _defaultMovieProfile.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.getDefaultTvQualityProfile().collect {
                _defaultTvProfile.value = it
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

        viewModelScope.launch {
            subscriptionRepository.getSubscriptionStatus().collect {
                _subscriptionStatus.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.getVibrantThemeColors().collect {
                _vibrantThemeColors.value = it
            }
        }

        viewModelScope.launch {
            // Check global settings
            val globalResult = settingsRepository.getGlobalNotificationSettings()
            if (globalResult is app.lusk.underseerr.domain.model.Result.Success) {
                _globalWebPushEnabled.value = globalResult.data
            }

            // Get Current User for permissions
            val userResult = authRepository.getCurrentUser()
            if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
                _currentUser.value = userResult.data
                
                // Sync notification settings from server if enabled
                val currentSettings = settingsRepository.getNotificationSettings().first()
                if (currentSettings.syncEnabled) {
                     val syncResult = notificationRepository.fetchRemoteSettings(userResult.data.id)
                     if (syncResult is app.lusk.underseerr.domain.model.Result.Success) {
                         val merged = syncResult.data.copy(syncEnabled = true)
                         settingsRepository.updateNotificationSettings(merged)
                     }
                }
            }
        }
    }

    private fun fetchProfiles() {
        viewModelScope.launch {
            // Fetch Movie Profiles
            val movieResult = requestRepository.getQualityProfiles(isMovie = true)
            if (movieResult is app.lusk.underseerr.domain.model.Result.Success) {
                _movieProfiles.value = movieResult.data
            }

            // Fetch TV Profiles
            val tvResult = requestRepository.getQualityProfiles(isMovie = false)
            if (tvResult is app.lusk.underseerr.domain.model.Result.Success) {
                _tvProfiles.value = tvResult.data
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
            if (settings.enabled && !_notificationSettings.value.enabled) {
                // Toggling ON
                val permission = app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS
                
                if (permissionManager.isPermissionGranted(permission)) {
                     settingsRepository.updateNotificationSettings(settings)
                     
                     // Force re-registration to ensure server Web Push setting is enabled
                     val cachedToken = settingsRepository.getPushToken().first()
                     if (cachedToken != null) {
                        launch {
                            notificationRepository.registerForPushNotifications(cachedToken)
                        }
                     }
                } else {
                    val shouldShowRationale = permissionManager.shouldShowRationale(permission)
                    val hasRequested = settingsRepository.hasRequestedNotificationPermission().first()
                    
                    if (shouldShowRationale) {
                        // User denied once. Requesting will show dialog.
                        permissionManager.requestPermission(permission)
                        settingsRepository.setHasRequestedNotificationPermission(true)
                        settingsRepository.updateNotificationSettings(settings)
                    } else {
                        if (!hasRequested) {
                            // First time. Requesting will show dialog.
                            permissionManager.requestPermission(permission)
                            settingsRepository.setHasRequestedNotificationPermission(true)
                            settingsRepository.updateNotificationSettings(settings)
                        } else {
                            // User denied permanently (or "Don't ask again").
                            // Dialog will NOT show. Open Settings.
                            permissionManager.openAppSettings()
                            // We don't update settings to true here, or we do and let sync handle it?
                            // Better to set it true so the toggle reflects the intent while they go to settings.
                            settingsRepository.updateNotificationSettings(settings)
                        }
                    }
                }
            } else {
                 settingsRepository.updateNotificationSettings(settings)
            }
        }
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        if (enabled && !_isBiometricAvailable.value) {
            // Cannot enable if not available
            return
        }
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }
    
    fun setDefaultMovieProfile(profileId: Int?) {
        viewModelScope.launch {
            settingsRepository.setDefaultMovieQualityProfile(profileId)
        }
    }

    fun setDefaultTvProfile(profileId: Int?) {
        viewModelScope.launch {
            settingsRepository.setDefaultTvQualityProfile(profileId)
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

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
        }
    }

    fun hasPermission(user: UserProfile?, permission: Int): Boolean {
        val userToCheck = user ?: return false
        val userPerms = userToCheck.rawPermissions
        // Check if ADMIN or specific permission
        return (userPerms and AppPermissions.ADMIN.toLong()) != 0L || 
               (userPerms and permission.toLong()) != 0L
    }

    private val _notificationServerUrl = MutableStateFlow<String?>(null)
    val notificationServerUrl: StateFlow<String?> = _notificationServerUrl.asStateFlow()

    init {
        loadSettings()
        fetchProfiles()
        checkBiometricAvailability()
        
        viewModelScope.launch {
            settingsRepository.getNotificationServerUrl().collect {
                _notificationServerUrl.value = it
            }
        }
    }

    // ... (rest of methods)

    fun setNotificationServerUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setNotificationServerUrl(url)
        }
    }

    fun configureWebhook() {
        viewModelScope.launch {
            // Use configured URL or default
            val currentUrl = _notificationServerUrl.value
            
            // Determine default logic
            val defaultEndpoint = if (app.lusk.underseerr.shared.BuildKonfig.DEBUG) {
                 app.lusk.underseerr.shared.BuildKonfig.WORKER_ENDPOINT_STAGING
            } else {
                 app.lusk.underseerr.shared.BuildKonfig.WORKER_ENDPOINT_PROD
            }
            
            val baseUrl = if (currentUrl.isNullOrBlank()) defaultEndpoint else currentUrl
            val sanitizedBase = baseUrl.trimEnd('/')
            val webhookUrl = "$sanitizedBase/webhook"
            
            val result = notificationRepository.updateWebhookSettings(webhookUrl)
            
            if (result is app.lusk.underseerr.domain.model.Result.Success) {
                _uiEvent.emit("Webhook configured successfully")
            } else if (result is app.lusk.underseerr.domain.model.Result.Error) {
                _uiEvent.emit("Failed: ${result.error.message ?: "Unknown error"}")
            }
        }
    }

    fun purchasePremium() {
        viewModelScope.launch {
            subscriptionRepository.purchasePremium()
            _uiEvent.emit("Premium unlocked!")
        }
    }

    fun updateVibrantThemeColors(colors: app.lusk.underseerr.domain.repository.VibrantThemeColors) {
        viewModelScope.launch {
            settingsRepository.updateVibrantThemeColors(colors)
        }
    }
}
