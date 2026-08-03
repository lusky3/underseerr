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
import app.lusk.underseerr.util.nowMillis

/** Type of trial prompt to show to the user. */
sealed class TrialPromptType {
    /** User has never started a trial — offer to activate it. */
    object Activate : TrialPromptType()
    /** User's trial has expired — offer to reset it (only shown if they haven't had premium). */
    object Reset : TrialPromptType()
    /** User's premium subscription expired — offer to renew (no trial reset option). */
    object PremiumExpired : TrialPromptType()
}


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

    private val _notificationServerUrl = MutableStateFlow<String?>(null)
    val notificationServerUrl: StateFlow<String?> = _notificationServerUrl.asStateFlow()

    private val _notificationServerType = MutableStateFlow("HOSTED")
    val notificationServerType: StateFlow<String> = _notificationServerType.asStateFlow()

    private val _webhookSecret = MutableStateFlow<String?>(null)
    val webhookSecret: StateFlow<String?> = _webhookSecret.asStateFlow()

    private val _homeScreenConfig = MutableStateFlow(app.lusk.underseerr.domain.repository.HomeScreenConfig())
    val homeScreenConfig: StateFlow<app.lusk.underseerr.domain.repository.HomeScreenConfig> = _homeScreenConfig.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent

    /** Non-null when a trial prompt should be shown; null when dismissed or not applicable. */
    private val _showTrialPrompt = MutableStateFlow<TrialPromptType?>(null)
    val showTrialPrompt: StateFlow<TrialPromptType?> = _showTrialPrompt

    /** Days remaining in the active trial, or null if not on a trial. */
    private val _trialDaysRemaining = MutableStateFlow<Int?>(null)
    val trialDaysRemaining: StateFlow<Int?> = _trialDaysRemaining

    // Suppresses the Reset prompt briefly after startTrial() to prevent race condition
    private var _trialJustStarted = false
    
    init {
        loadSettings()
        fetchProfiles()
        checkBiometricAvailability()
        
        viewModelScope.launch {
            settingsRepository.getNotificationServerUrl().collect {
                _notificationServerUrl.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.getNotificationServerType().collect {
                _notificationServerType.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.getWebhookSecret().collect {
                _webhookSecret.value = it
            }
        }

        // Single authoritative subscription + trial lifecycle collector.
        viewModelScope.launch {
            subscriptionRepository.getSubscriptionStatus().collect { status ->
                _subscriptionStatus.value = status

                when (status.tier) {
                    app.lusk.underseerr.domain.model.SubscriptionTier.TRIAL -> {
                        // Race guard complete — trial tier is confirmed
                        _trialJustStarted = false
                        // Compute and expose days remaining
                        val remaining = status.expiresAt?.let { exp ->
                            val ms = exp - nowMillis()
                            if (ms > 0) ((ms / (1000L * 60 * 60 * 24)) + 1).toInt() else 0
                        }
                        _trialDaysRemaining.value = remaining
                        // Clear any lingering prompt — trial is active
                        if (_showTrialPrompt.value is TrialPromptType.Activate) {
                            _showTrialPrompt.value = null
                        }
                    }
                    app.lusk.underseerr.domain.model.SubscriptionTier.FREE -> {
                        _trialDaysRemaining.value = null
                        // Skip prompt logic if we just started a trial (race: flow
                        // may emit FREE multiple times before settling on TRIAL)
                        if (_trialJustStarted) {
                            return@collect
                        }
                        
                        // Check if prompt was recently dismissed (within 7 days)
                        val dismissedAt = settingsRepository.getTrialPromptDismissedAt().first()
                        val daysSinceDismissal = (nowMillis() - dismissedAt) / (1000L * 60 * 60 * 24)
                        if (dismissedAt > 0 && daysSinceDismissal < 7) {
                            // User dismissed within last 7 days — don't show again
                            return@collect
                        }
                        
                        val trialStart = settingsRepository.getTrialStartDate().first()
                        val hasUsedTrial = settingsRepository.getHasUsedTrial().first()
                        val premiumExpiresAt = status.expiresAt
                        
                        // Check if this is an expired premium subscription (had premium that expired)
                        // We detect this by checking if premiumExpiresAt is set and in the past
                        val hadPremiumThatExpired = premiumExpiresAt != null && premiumExpiresAt < nowMillis()
                        
                        if (hadPremiumThatExpired) {
                            // User had premium that expired — show PremiumExpired prompt
                            val currentType = settingsRepository.getNotificationServerType().first()
                            if (currentType == "HOSTED") {
                                settingsRepository.setNotificationServerType("NONE")
                            }
                            if (_showTrialPrompt.value == null) {
                                _showTrialPrompt.value = TrialPromptType.PremiumExpired
                            }
                        } else if (trialStart != null || hasUsedTrial) {
                            // Trial was started but is now FREE -> it expired
                            val currentType = settingsRepository.getNotificationServerType().first()
                            if (currentType == "HOSTED") {
                                settingsRepository.setNotificationServerType("NONE")
                            }
                            // Only show reset prompt if not already showing something
                            if (_showTrialPrompt.value == null) {
                                _showTrialPrompt.value = TrialPromptType.Reset
                            }
                        } else {
                            // Completely new user who has never used a trial — offer trial activation
                            if (_showTrialPrompt.value == null && !status.isPremium) {
                                _showTrialPrompt.value = TrialPromptType.Activate
                            }
                        }
                    }
                    app.lusk.underseerr.domain.model.SubscriptionTier.PREMIUM -> {
                        _trialDaysRemaining.value = null
                        _showTrialPrompt.value = null
                    }
                }
            }
        }
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
            settingsRepository.getNotificationSettings().collect { settings ->
                // Sync enabled state with actual permission status
                val permission = app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS
                val isGranted = permissionManager.isPermissionGranted(permission)
                
                // If permission is not granted but settings show enabled, disable it
                if (settings.enabled && !isGranted) {
                    val correctedSettings = settings.copy(enabled = false)
                    _notificationSettings.value = correctedSettings
                    // Also persist the corrected state
                    settingsRepository.updateNotificationSettings(correctedSettings)
                } else {
                    _notificationSettings.value = settings
                }
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

        // Note: subscription status is collected once in init{} — do not add a second collector here.

        viewModelScope.launch {
            settingsRepository.getVibrantThemeColors().collect {
                _vibrantThemeColors.value = it
            }
        }

        viewModelScope.launch {
            settingsRepository.getHomeScreenConfig().collect {
                _homeScreenConfig.value = it
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
                     val webhookSecret = settingsRepository.getWebhookSecret().first()
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
            val defaultEndpoint = app.lusk.underseerr.util.defaultWorkerEndpoint
            
            val baseUrl = if (currentUrl.isNullOrBlank()) defaultEndpoint else currentUrl
            val sanitizedBase = baseUrl.trimEnd('/')
            val webhookUrl = "$sanitizedBase/webhook"
            
            val result = notificationRepository.updateWebhookSettings(webhookUrl)
            
            if (result is app.lusk.underseerr.domain.model.Result.Success) {
                _uiEvent.emit("Webhook configured successfully")
            } else if (result is app.lusk.underseerr.domain.model.Result.Error) {
                _uiEvent.emit("Failed: ${result.error.message}")
            }
        }
    }

    fun purchasePremium(isYearly: Boolean = false) {
        viewModelScope.launch {
            subscriptionRepository.purchasePremium(isYearly)
            // Note: Success message will be handled by the subscription status update
        }
    }

    fun setNotificationServerType(type: String) {
        viewModelScope.launch {
            settingsRepository.setNotificationServerType(type)
        }
    }

    /**
     * Explicitly starts (or resets) the 30-day notification trial.
     * Writes the local timestamp AND registers the trial on the server so
     * notifications are actually delivered during the trial period.
     */
    fun startTrial() {
        viewModelScope.launch {
            // 1. Suppress race condition: collector may briefly see FREE before TRIAL
            _trialJustStarted = true
            // 2. Set local start date for immediate UI reactivity
            settingsRepository.setTrialStartDate(nowMillis())
            // 2b. Mark that user has used a trial (persists even if trialStartDate is cleared)
            settingsRepository.setHasUsedTrial(true)
            // 3. Activate hosted server so notifications work immediately
            val currentType = settingsRepository.getNotificationServerType().first()
            if (currentType != "HOSTED") {
                settingsRepository.setNotificationServerType("HOSTED")
            }
            _showTrialPrompt.value = null
            // 4. Register trial on the backend — required for notification delivery gating
            val serverResult = subscriptionRepository.startTrialOnServer()
            if (serverResult.isFailure) {
                // Local trial still active; server will retry on next subscription refresh.
                _uiEvent.emit("Trial started! Note: server sync failed — notifications may be delayed until connectivity is restored.")
            }
        }
    }

    fun dismissTrialPrompt() {
        _showTrialPrompt.value = null
        // Persist dismissal so prompt doesn't reappear for 7 days
        viewModelScope.launch {
            settingsRepository.setTrialPromptDismissedAt(nowMillis())
        }
    }

    /** Launches the Google Play trial offer purchase flow. */
    fun purchasePremiumWithTrial() {
        viewModelScope.launch {
            subscriptionRepository.purchasePremiumWithTrial()
        }
    }

    fun unlockWithSerialKey(key: String) {
        viewModelScope.launch {
            val result = subscriptionRepository.unlockWithSerialKey(key)
            if (result.isSuccess) {
                _uiEvent.emit("Premium features unlocked!")
            } else {
                _uiEvent.emit("Error: ${result.exceptionOrNull()?.message ?: "Invalid key"}")
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            val result = subscriptionRepository.restorePurchases()
            if (result.isSuccess) {
                _uiEvent.emit("Subscription restored successfully!")
            } else {
                _uiEvent.emit(result.exceptionOrNull()?.message ?: "Restore failed")
            }
        }
    }
    
    fun updateVibrantThemeColors(colors: app.lusk.underseerr.domain.repository.VibrantThemeColors) {
        viewModelScope.launch {
            settingsRepository.updateVibrantThemeColors(colors)
        }
    }

    fun updateWebhookSecret(secret: String?) {
        viewModelScope.launch {
            settingsRepository.updateWebhookSecret(secret)
            // Trigger re-registration to update server
            val token = settingsRepository.getPushToken().first()
            if (token != null) {
                notificationRepository.registerForPushNotifications(token)
            }
        }
    }

    fun generateWebhookSecret() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val secret = (1..16)
            .map { chars.random() }
            .joinToString("")
        updateWebhookSecret(secret)
    }

    fun updateHomeScreenConfig(config: app.lusk.underseerr.domain.repository.HomeScreenConfig) {
        viewModelScope.launch {
            settingsRepository.updateHomeScreenConfig(config)
        }
    }
}
