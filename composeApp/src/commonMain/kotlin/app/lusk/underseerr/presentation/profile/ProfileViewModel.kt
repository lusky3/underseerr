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
    
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _quota = MutableStateFlow<app.lusk.underseerr.domain.repository.RequestQuota?>(null)
    val quota: StateFlow<app.lusk.underseerr.domain.repository.RequestQuota?> = _quota.asStateFlow()

    private val _statistics = MutableStateFlow<app.lusk.underseerr.domain.model.UserStatistics?>(null)
    val statistics: StateFlow<app.lusk.underseerr.domain.model.UserStatistics?> = _statistics.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshingStats = MutableStateFlow(false)
    val isRefreshingStats: StateFlow<Boolean> = _isRefreshingStats.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            if (_profile.value == null) {
                _isLoading.value = true
            } else {
                _isRefreshingStats.value = true
            }
            _error.value = null
            
            // Fetch everything in parallel
            val profileJob = launch {
                when (val result = profileRepository.getUserProfile()) {
                    is Result.Success -> _profile.value = result.data
                    is Result.Error -> _error.value = result.error.getUserMessage()
                    is Result.Loading -> {}
                }
            }
            
            val quotaJob = launch {
                when (val result = profileRepository.getUserQuota()) {
                    is Result.Success -> _quota.value = result.data
                    is Result.Error -> _error.value = _error.value ?: result.error.getUserMessage()
                    is Result.Loading -> {}
                }
            }
            
            val statsJob = launch {
                when (val result = profileRepository.getUserStatistics()) {
                    is Result.Success -> _statistics.value = result.data
                    is Result.Error -> _error.value = _error.value ?: result.error.getUserMessage()
                    is Result.Loading -> {}
                }
            }

            // Sync
            listOf(profileJob, quotaJob, statsJob).forEach { it.join() }
            
            // Background refresh requests to update stats
            requestRepository.refreshRequests()
            
            // Re-fetch stats after sync
            val finalStats = profileRepository.getUserStatistics()
            if (finalStats is Result.Success) {
                _statistics.value = finalStats.data
            }
            
            _isLoading.value = false
            _isRefreshingStats.value = false
        }
    }
    
    fun refresh() {
        loadProfile()
    }

    fun refreshStatistics() {
        viewModelScope.launch {
            _isRefreshingStats.value = true
            requestRepository.refreshRequests()
            val finalStats = profileRepository.getUserStatistics()
            if (finalStats is Result.Success) {
                _statistics.value = finalStats.data
            }
            _isRefreshingStats.value = false
        }
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
