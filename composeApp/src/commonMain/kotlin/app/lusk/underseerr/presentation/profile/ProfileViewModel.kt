package app.lusk.underseerr.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.model.UserStatistics
import app.lusk.underseerr.domain.repository.ProfileRepository
import app.lusk.underseerr.domain.repository.RequestQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for user profile screen.
 * Feature: underseerr
 * Validates: Requirements 5.1
 */
/**
 * ViewModel for user profile screen.
 * Feature: underseerr
 * Validates: Requirements 5.1
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val requestRepository: app.lusk.underseerr.domain.repository.RequestRepository
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
                _profileState.value = ProfileState.Error(profileResult.error.getUserMessage())
                return@launch
            } else if (quotaResult is Result.Error) {
                 _profileState.value = ProfileState.Error(quotaResult.error.getUserMessage())
                 return@launch
            } else if (statsResult is Result.Error) {
                 _profileState.value = ProfileState.Error(statsResult.error.getUserMessage())
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
