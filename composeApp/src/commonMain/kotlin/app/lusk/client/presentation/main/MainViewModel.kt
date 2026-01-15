package app.lusk.client.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.repository.SettingsRepository
import app.lusk.client.domain.repository.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for main activity to manage app-wide state.
 */
class MainViewModel(
    settingsRepository: SettingsRepository
) : ViewModel() {
    
    val themePreference: StateFlow<ThemePreference> = settingsRepository.getThemePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM
        )

    val isBiometricEnabled: StateFlow<Boolean> = settingsRepository.getBiometricEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
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
}
