package app.lusk.client.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.model.UserStatistics
import app.lusk.client.domain.repository.ProfileRepository
import app.lusk.client.domain.repository.RequestQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for user profile screen.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.1
 */
/**
 * ViewModel for user profile screen.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.1
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val requestRepository: app.lusk.client.domain.repository.RequestRepository
) : ViewModel() {
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            
            // Fetch initial data (fast)
            val profileResult = profileRepository.getUserProfile()
            val quotaResult = profileRepository.getUserQuota()
            var statsResult = profileRepository.getUserStatistics()
            
            // Show result immediately
            if (profileResult is Result.Success && 
                quotaResult is Result.Success && 
                statsResult is Result.Success) {
                _profileState.value = ProfileState.Success(
                    profile = profileResult.data,
                    quota = quotaResult.data,
                    statistics = statsResult.data
                )
            } else if (profileResult is Result.Error) {
                _profileState.value = ProfileState.Error(profileResult.error.toString())
                return@launch
            } else if (quotaResult is Result.Error) {
                 _profileState.value = ProfileState.Error(quotaResult.error.toString())
                 return@launch
            } else if (statsResult is Result.Error) {
                 _profileState.value = ProfileState.Error(statsResult.error.toString())
                 return@launch
            }
            
            // Refresh requests in background (slow due to hydration)
            // This ensures local database is up to date for stats fallback
            requestRepository.refreshRequests()
            
            // Re-fetch stats (might have improved if using fallback)
            statsResult = profileRepository.getUserStatistics()
            
            if (profileResult is Result.Success && 
                quotaResult is Result.Success && 
                statsResult is Result.Success) {
                _profileState.value = ProfileState.Success(
                    profile = profileResult.data,
                    quota = quotaResult.data,
                    statistics = statsResult.data
                )
            }
        }
    }
    
    fun refresh() {
        loadProfile()
    }
}

/**
 * UI state for profile screen.
 */
sealed class ProfileState {
    data object Loading : ProfileState()
    data class Success(
        val profile: UserProfile,
        val quota: RequestQuota,
        val statistics: UserStatistics
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
