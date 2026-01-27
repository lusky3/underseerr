package app.lusk.underseerr.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for main activity to manage app-wide state.
 */
class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val permissionManager: app.lusk.underseerr.domain.permission.PermissionManager
) : ViewModel() {
    
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
            settingsRepository.getNotificationSettings().collect { settings ->
                if (settings.enabled) {
                    if (!permissionManager.isPermissionGranted(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)) {
                        permissionManager.requestPermission(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)
                    }
                }
                // Cancellation is important here because collect is infinite
                throw kotlinx.coroutines.CancellationException()
            }
        }
    }
    
    fun syncNotificationState() {
        viewModelScope.launch {
             // If System Permission is DENIED, but Settings is ENABLED -> Disable Settings
             // This ensures "If denied, toggle off"
             val isGranted = permissionManager.isPermissionGranted(app.lusk.underseerr.domain.permission.Permission.NOTIFICATIONS)
             if (!isGranted) {
                 settingsRepository.getNotificationSettings().collect { settings ->
                     if (settings.enabled) {
                        settingsRepository.updateNotificationSettings(settings.copy(enabled = false))
                     }
                     throw kotlinx.coroutines.CancellationException()
                 }
             }
        }
    }
}
