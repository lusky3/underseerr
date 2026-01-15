package app.lusk.client.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.model.MediaRequest
import app.lusk.client.domain.model.RequestStatus
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.repository.QualityProfile
import app.lusk.client.domain.repository.RequestRepository
import app.lusk.client.domain.repository.RootFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * ViewModel for request screens.
 * Feature: overseerr-android-client
 * Validates: Requirements 3.1, 3.2, 3.3, 4.1, 4.4
 */
/**
 * ViewModel for request screens.
 * Feature: overseerr-android-client
 * Validates: Requirements 3.1, 3.2, 3.3, 4.1, 4.4
 */
class RequestViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {
    
    // User requests
    private val _userRequests = MutableStateFlow<List<MediaRequest>>(emptyList())
    val userRequests: StateFlow<List<MediaRequest>> = _userRequests.asStateFlow()
    
    // Request submission state
    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState.asStateFlow()
    
    // Quality profiles
    private val _qualityProfiles = MutableStateFlow<List<QualityProfile>>(emptyList())
    val qualityProfiles: StateFlow<List<QualityProfile>> = _qualityProfiles.asStateFlow()
    
    // Root folders
    private val _rootFolders = MutableStateFlow<List<RootFolder>>(emptyList())
    val rootFolders: StateFlow<List<RootFolder>> = _rootFolders.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Partial requests enabled state
    private val _partialRequestsEnabled = MutableStateFlow(false)
    val partialRequestsEnabled: StateFlow<Boolean> = _partialRequestsEnabled.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        viewModelScope.launch {
            requestRepository.getUserRequests().collect { requests ->
                _userRequests.value = requests
            }
        }

        refreshRequests()
        
        viewModelScope.launch {
            val result = requestRepository.getPartialRequestsEnabled()
            if (result.isSuccess) {
                _partialRequestsEnabled.value = result.getOrDefault(false)
            }
        }
    }
    
    /**
     * Load user requests.
     * Property 13: Request List Completeness
     */
    private fun loadUserRequests() {
        viewModelScope.launch {
            requestRepository.getUserRequests().collect { requests ->
                _userRequests.value = requests
            }
        }
    }
    
    /**
     * Submit a movie request.
     * Property 9: Request Submission Completeness
     * Property 10: Request Confirmation Display
     */
    fun submitMovieRequest(
        movieId: Int,
        qualityProfile: Int? = null,
        rootFolder: String? = null
    ) {
        viewModelScope.launch {
            _requestState.value = RequestState.Loading
            _error.value = null
            
            when (val result = requestRepository.requestMovie(movieId, qualityProfile, rootFolder)) {
                is Result.Success -> {
                    _requestState.value = RequestState.Success(result.data)
                }
                is Result.Error -> {
                    _requestState.value = RequestState.Error(result.error.message)
                    _error.value = result.error.message
                }
                is Result.Loading -> {
                    _requestState.value = RequestState.Loading
                }
            }
        }
    }
    
    /**
     * Submit a TV show request.
     * Property 9: Request Submission Completeness
     * Property 10: Request Confirmation Display
     */
    fun submitTvShowRequest(
        tvShowId: Int,
        seasons: List<Int>,
        qualityProfile: Int? = null,
        rootFolder: String? = null
    ) {
        viewModelScope.launch {
            _requestState.value = RequestState.Loading
            _error.value = null
            
            when (val result = requestRepository.requestTvShow(tvShowId, seasons, qualityProfile, rootFolder)) {
                is Result.Success -> {
                    _requestState.value = RequestState.Success(result.data)
                }
                is Result.Error -> {
                    _requestState.value = RequestState.Error(result.error.message)
                    _error.value = result.error.message
                }
                is Result.Loading -> {
                    _requestState.value = RequestState.Loading
                }
            }
        }
    }
    
    /**
     * Cancel a request.
     * Property 16: Permission-Based Cancellation
     */
    fun cancelRequest(requestId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = requestRepository.cancelRequest(requestId)) {
                is Result.Success -> {
                    // Request cancelled successfully
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _error.value = result.error.message
                    _isLoading.value = false
                }
                is Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Refresh requests from server.
     * Property 18: Pull-to-Refresh Data Freshness
     */
    fun refreshRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = requestRepository.refreshRequests()) {
                is Result.Success -> {
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _error.value = result.error.message
                    _isLoading.value = false
                }
                is Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    /**
     * Check if media is already requested.
     * Property 11: Duplicate Request Prevention
     */
    fun checkIfMediaRequested(mediaId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val result = requestRepository.isMediaRequested(mediaId)) {
                is Result.Success -> {
                    onResult(result.data)
                }
                is Result.Error -> {
                    _error.value = result.error.message
                    onResult(false)
                }
                is Result.Loading -> {
                    // Loading
                }
            }
        }
    }
    
    // Options loading state
    private val _isQualityProfilesLoading = MutableStateFlow(false)
    private val _isRootFoldersLoading = MutableStateFlow(false)
    
    val isOptionsLoading: StateFlow<Boolean> = combine(
        _isQualityProfilesLoading, 
        _isRootFoldersLoading
    ) { q, r -> q || r }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Load quality profiles.
     * Property 12: Advanced Options Availability
     */
    /**
     * Load quality profiles.
     * Property 12: Advanced Options Availability
     */
    fun loadQualityProfiles(isMovie: Boolean) {
        _isQualityProfilesLoading.value = true
        _qualityProfiles.value = emptyList() // Clear immediately
        viewModelScope.launch {
            when (val result = requestRepository.getQualityProfiles(isMovie)) {
                is Result.Success -> {
                    // Filter out any potential blank profiles from the source
                    _qualityProfiles.value = result.data.filter { it.name.isNotBlank() }
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
                is Result.Loading -> {}
            }
            _isQualityProfilesLoading.value = false
        }
    }
    
    /**
     * Load root folders.
     * Property 12: Advanced Options Availability
     */
    fun loadRootFolders(isMovie: Boolean) {
        _isRootFoldersLoading.value = true
        _rootFolders.value = emptyList() // Clear immediately
        viewModelScope.launch {
            when (val result = requestRepository.getRootFolders(isMovie)) {
                is Result.Success -> {
                    // Filter out any potential blank folders from the source
                    _rootFolders.value = result.data.filter { it.path.isNotBlank() }
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
                is Result.Loading -> {}
            }
            _isRootFoldersLoading.value = false
        }
    }
    
    /**
     * Get requests by status.
     * Property 14: Request Grouping Correctness
     */
    fun getRequestsByStatus(status: RequestStatus): StateFlow<List<MediaRequest>> {
        val stateFlow = MutableStateFlow<List<MediaRequest>>(emptyList())
        viewModelScope.launch {
            requestRepository.getRequestsByStatus(status).collect { requests ->
                stateFlow.value = requests
            }
        }
        return stateFlow.asStateFlow()
    }
    
    /**
     * Clear request state.
     */
    fun clearRequestState() {
        _requestState.value = RequestState.Idle
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Request state.
 */
sealed class RequestState {
    data object Idle : RequestState()
    data object Loading : RequestState()
    data class Success(val request: MediaRequest) : RequestState()
    data class Error(val message: String) : RequestState()
}
