package app.lusk.client.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.model.UserStatistics
import app.lusk.client.domain.repository.ProfileRepository
import app.lusk.client.domain.repository.RequestQuota
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for user profile screen.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.1
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
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
            
            // Refresh requests first to ensure statistics are accurate
            requestRepository.refreshRequests()
            
            val profileResult = profileRepository.getUserProfile()
            val quotaResult = profileRepository.getUserQuota()
            val statsResult = profileRepository.getUserStatistics()
            
            when {
                profileResult is Result.Success && 
                quotaResult is Result.Success && 
                statsResult is Result.Success -> {
                    _profileState.value = ProfileState.Success(
                        profile = profileResult.data,
                        quota = quotaResult.data,
                        statistics = statsResult.data
                    )
                }
                profileResult is Result.Error -> {
                    _profileState.value = ProfileState.Error(profileResult.error.toString())
                }
                quotaResult is Result.Error -> {
                    _profileState.value = ProfileState.Error(quotaResult.error.toString())
                }
                statsResult is Result.Error -> {
                    _profileState.value = ProfileState.Error(statsResult.error.toString())
                }
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
