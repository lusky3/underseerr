package app.lusk.underseerr.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for main activity to manage app-wide state.
 */
class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: app.lusk.underseerr.domain.repository.NotificationRepository,
    private val permissionManager: app.lusk.underseerr.domain.permission.PermissionManager
) : ViewModel() {
    
    fun registerPushToken(token: String) {
        viewModelScope.launch {
            settingsRepository.savePushToken(token)
            notificationRepository.registerForPushNotifications(token)
        }
    }
    
    val themePreference: StateFlow<ThemePreference> = settingsRepository.getThemePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM
        )

    val isBiometricEnabled: StateFlow<Boolean?> = settingsRepository.getBiometricEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val vibrantThemeColors: StateFlow<app.lusk.underseerr.domain.repository.VibrantThemeColors> = settingsRepository.getVibrantThemeColors()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = app.lusk.underseerr.domain.repository.VibrantThemeColors()
        )

    private val _isAppLocked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var hasInitialLockCheckBeenDone = false

    fun checkInitialLockState(biometricEnabled: Boolean) {
        if (!hasInitialLockCheckBeenDone) {
            if (biometricEnabled) {
                _isAppLocked.value = true
            }
            hasInitialLockCheckBeenDone = true
        }
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }
    
    fun requestNotificationPermission() {
        viewModelScope.launch {
            // Check current setting
            val settings = settingsRepository.getNotificationSettings().firstOrNull() ?: return@launch
            if (settings.enabled) {
                if (!permissionManager.isPermissionGranted(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)) {
                    permissionManager.requestPermission(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)
                }
            }
        }
    }
    
    fun syncNotificationState() {
        viewModelScope.launch {
            val isGranted = permissionManager.isPermissionGranted(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)
            val settings = settingsRepository.getNotificationSettings().firstOrNull() ?: return@launch
            
            if (isGranted && !settings.enabled) {
                // System granted but app disabled -> Auto-enable app setting
                settingsRepository.updateNotificationSettings(settings.copy(enabled = true))
            } else if (!isGranted && settings.enabled) {
                // System denied but app enabled -> Toggle off app setting
                settingsRepository.updateNotificationSettings(settings.copy(enabled = false))
            }
        }
    }
    private val _navigationEvent = kotlinx.coroutines.flow.MutableSharedFlow<app.lusk.underseerr.navigation.Screen>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun handleDeepLink(uri: String) {
        val screen = app.lusk.underseerr.navigation.Screen.parseDeepLink(uri)
        if (screen != null) {
            _navigationEvent.tryEmit(screen)
        }
    }

    private val _selectedTab = kotlinx.coroutines.flow.MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    private val _navCommand = kotlinx.coroutines.flow.MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val navCommand = _navCommand.asSharedFlow()

    fun navigateToTab(index: Int) {
        _selectedTab.value = index
        _navCommand.tryEmit(index)
    }

    private val _tabDragEvent = kotlinx.coroutines.flow.MutableSharedFlow<Float>()
    val tabDragEvent = _tabDragEvent.asSharedFlow()

    fun emitTabDrag(delta: Float) {
        viewModelScope.launch {
            _tabDragEvent.emit(delta)
        }
    }

    private val _tabDragEnd = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val tabDragEnd = _tabDragEnd.asSharedFlow()

    fun emitTabDragEnd() {
        viewModelScope.launch {
            _tabDragEnd.emit(Unit)
        }
    }
    
    // Global filter state for Requests tab
    private val _requestsFilter = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val requestsFilter: StateFlow<String?> = _requestsFilter.asStateFlow()
    
    fun setRequestsFilter(filter: String?) {
        _requestsFilter.value = filter
    }
}
