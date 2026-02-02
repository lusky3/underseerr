package app.lusk.underseerr.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.RequestStatus
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.QualityProfile
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.RootFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class RequestViewModel(
    private val requestRepository: RequestRepository,
    private val settingsRepository: app.lusk.underseerr.domain.repository.SettingsRepository
) : ViewModel() {
    
    private val _userRequests = MutableStateFlow<List<MediaRequest>>(emptyList())
    val userRequests: StateFlow<List<MediaRequest>> = _userRequests.asStateFlow()
    
    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState.asStateFlow()
    
    private val _qualityProfiles = MutableStateFlow<List<QualityProfile>>(emptyList())
    val qualityProfiles: StateFlow<List<QualityProfile>> = _qualityProfiles.asStateFlow()
    
    private val _rootFolders = MutableStateFlow<List<RootFolder>>(emptyList())
    val rootFolders: StateFlow<List<RootFolder>> = _rootFolders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _partialRequestsEnabled = MutableStateFlow(false)
    val partialRequestsEnabled: StateFlow<Boolean> = _partialRequestsEnabled.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val defaultMovieProfile = settingsRepository.getDefaultMovieQualityProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
        
    val defaultTvProfile = settingsRepository.getDefaultTvQualityProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
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
    
    fun submitMovieRequest(movieId: Int, qualityProfile: Int? = null, rootFolder: String? = null) {
        viewModelScope.launch {
            _requestState.value = RequestState.Loading
            _error.value = null
            when (val result = requestRepository.requestMovie(movieId, qualityProfile, rootFolder)) {
                is Result.Success -> _requestState.value = RequestState.Success(result.data)
                is Result.Error -> {
                    _requestState.value = RequestState.Error(result.error.message)
                    _error.value = result.error.message
                }
                is Result.Loading -> _requestState.value = RequestState.Loading
            }
        }
    }
    
    fun submitTvShowRequest(tvShowId: Int, seasons: List<Int>, qualityProfile: Int? = null, rootFolder: String? = null) {
        viewModelScope.launch {
            _requestState.value = RequestState.Loading
            _error.value = null
            when (val result = requestRepository.requestTvShow(tvShowId, seasons, qualityProfile, rootFolder)) {
                is Result.Success -> _requestState.value = RequestState.Success(result.data)
                is Result.Error -> {
                    _requestState.value = RequestState.Error(result.error.message)
                    _error.value = result.error.message
                }
                is Result.Loading -> _requestState.value = RequestState.Loading
            }
        }
    }
    
    fun cancelRequest(requestId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = requestRepository.cancelRequest(requestId)) {
                is Result.Success -> _isLoading.value = false
                is Result.Error -> {
                    _error.value = result.error.message
                    _isLoading.value = false
                }
                is Result.Loading -> _isLoading.value = true
            }
        }
    }
    
    private var currentPage = 1
    private val pageSize = 10
    private var isLastPage = false

    fun refreshRequests(isPullToRefresh: Boolean = false) {
        currentPage = 1
        isLastPage = false
        loadRequests(1)
    }
    
    fun loadMoreRequests() {
        if (isLoading.value || isLastPage) return
        loadRequests(currentPage + 1)
    }

    private fun loadRequests(page: Int) {
        if (page > 1 && isLastPage) return

        println("RequestViewModel: loadRequests(page=$page, pageSize=$pageSize)")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = requestRepository.refreshRequests(page, pageSize)) {
                is Result.Success -> {
                    _isLoading.value = false
                    currentPage = page
                    val (itemsFetched, totalOnServer) = result.data
                    
                    val currentItemCount = (page - 1) * pageSize + itemsFetched
                    println("RequestViewModel: Success. Fetched $itemsFetched, total items seen so far: $currentItemCount/$totalOnServer")
                    
                    if (currentItemCount >= totalOnServer || itemsFetched == 0) {
                        isLastPage = true
                        println("RequestViewModel: Marked as last page")
                    } else {
                        isLastPage = false
                    }
                }
                is Result.Error -> {
                    _error.value = result.error.message
                    _isLoading.value = false
                    println("RequestViewModel: Error loading page $page: ${result.error.message}")
                }
                is Result.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun checkIfMediaRequested(mediaId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val result = requestRepository.isMediaRequested(mediaId)) {
                is Result.Success -> onResult(result.data)
                is Result.Error -> {
                    _error.value = result.error.message
                    onResult(false)
                }
                is Result.Loading -> {}
            }
        }
    }
    
    private val _isQualityProfilesLoading = MutableStateFlow(false)
    private val _isRootFoldersLoading = MutableStateFlow(false)
    
    val isOptionsLoading: StateFlow<Boolean> = combine(
        _isQualityProfilesLoading, 
        _isRootFoldersLoading
    ) { q, r -> q || r }
    .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun loadQualityProfiles(isMovie: Boolean) {
        _isQualityProfilesLoading.value = true
        _qualityProfiles.value = emptyList()
        viewModelScope.launch {
            when (val result = requestRepository.getQualityProfiles(isMovie)) {
                is Result.Success -> _qualityProfiles.value = result.data.filter { it.name.isNotBlank() }
                is Result.Error -> _error.value = result.error.message
                is Result.Loading -> {}
            }
            _isQualityProfilesLoading.value = false
        }
    }
    
    fun loadRootFolders(isMovie: Boolean) {
        _isRootFoldersLoading.value = true
        _rootFolders.value = emptyList()
        viewModelScope.launch {
            when (val result = requestRepository.getRootFolders(isMovie)) {
                is Result.Success -> _rootFolders.value = result.data.filter { it.path.isNotBlank() }
                is Result.Error -> _error.value = result.error.message
                is Result.Loading -> {}
            }
            _isRootFoldersLoading.value = false
        }
    }
    
    fun getRequestsByStatus(status: RequestStatus): StateFlow<List<MediaRequest>> {
        val stateFlow = MutableStateFlow<List<MediaRequest>>(emptyList())
        viewModelScope.launch {
            requestRepository.getRequestsByStatus(status).collect { requests ->
                stateFlow.value = requests
            }
        }
        return stateFlow.asStateFlow()
    }
    
    fun clearRequestState() {
        _requestState.value = RequestState.Idle
    }
    
    fun clearError() {
        _error.value = null
    }
}

sealed class RequestState {
    data object Idle : RequestState()
    data object Loading : RequestState()
    data class Success(val request: MediaRequest) : RequestState()
    data class Error(val message: String) : RequestState()
}
