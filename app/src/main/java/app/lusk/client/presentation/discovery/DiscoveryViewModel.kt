package app.lusk.client.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.model.Movie
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.TvShow
import app.lusk.client.domain.repository.DiscoveryRepository
import app.lusk.client.domain.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for discovery screens.
 * Feature: overseerr-android-client
 * Validates: Requirements 2.1, 2.2, 2.4
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {
    
    companion object {
        private const val SEARCH_DEBOUNCE_MS = 500L
    }
    
    // Trending content
    val trendingMovies: StateFlow<PagingData<Movie>> = 
        discoveryRepository.getTrendingMovies()
            .cachedIn(viewModelScope)
            .stateIn(viewModelScope, PagingData.empty())
    
    val trendingTvShows: StateFlow<PagingData<TvShow>> = 
        discoveryRepository.getTrendingTvShows()
            .cachedIn(viewModelScope)
            .stateIn(viewModelScope, PagingData.empty())
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    // Paged Search results
    val pagedSearchResults: Flow<PagingData<app.lusk.client.domain.model.SearchResult>> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                emptyFlow()
            } else {
                discoveryRepository.findMedia(query)
            }
        }
        .cachedIn(viewModelScope)
    
    // Media details state
    private val _mediaDetailsState = MutableStateFlow<MediaDetailsState>(MediaDetailsState.Idle)
    val mediaDetailsState: StateFlow<MediaDetailsState> = _mediaDetailsState.asStateFlow()

    // Partial requests setting
    private val _partialRequestsEnabled = MutableStateFlow(false)
    val partialRequestsEnabled: StateFlow<Boolean> = _partialRequestsEnabled.asStateFlow()
    
    init {
        // Fetch partial requests setting
        viewModelScope.launch {
            val result = requestRepository.getPartialRequestsEnabled()
            if (result.isSuccess) {
                _partialRequestsEnabled.value = result.getOrDefault(false)
            }
        }

        // Set up search debouncing
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }
    
    /**
     * Update search query.
     * Property 5: Search Performance (with debouncing)
     */
    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
        } else {
            _searchState.value = SearchState.Loading
        }
    }
    
    /**
     * Perform search.
     * Property 5: Search Performance
     * Property 6: Search Result Completeness
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            
            when (val result = discoveryRepository.searchMedia(query)) {
                is Result.Success -> {
                    _searchState.value = SearchState.Success(result.data.results)
                }
                is Result.Error -> {
                    _searchState.value = SearchState.Error(result.error.message)
                }
                is Result.Loading -> {
                    _searchState.value = SearchState.Loading
                }
            }
        }
    }
    
    /**
     * Load media details based on type.
     * Property 7: Media Detail Navigation
     */
    fun loadMediaDetails(mediaType: MediaType, mediaId: Int) {
        when (mediaType) {
            MediaType.MOVIE -> loadMovieDetails(mediaId)
            MediaType.TV -> loadTvShowDetails(mediaId)
        }
    }
    
    /**
     * Load movie details.
     * Property 7: Media Detail Navigation
     */
    private fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            _mediaDetailsState.value = MediaDetailsState.Loading
            
            when (val result = discoveryRepository.getMovieDetails(movieId)) {
                is Result.Success -> {
                    _mediaDetailsState.value = MediaDetailsState.Success(
                        details = result.data.toMediaDetails(_partialRequestsEnabled.value)
                    )
                }
                is Result.Error -> {
                    _mediaDetailsState.value = MediaDetailsState.Error(result.error.message)
                }
                is Result.Loading -> {
                    _mediaDetailsState.value = MediaDetailsState.Loading
                }
            }
        }
    }
    
    /**
     * Load TV show details.
     * Property 7: Media Detail Navigation
     */
    private fun loadTvShowDetails(tvShowId: Int) {
        viewModelScope.launch {
            _mediaDetailsState.value = MediaDetailsState.Loading
            
            when (val result = discoveryRepository.getTvShowDetails(tvShowId)) {
                is Result.Success -> {
                    _mediaDetailsState.value = MediaDetailsState.Success(
                        details = result.data.toMediaDetails(_partialRequestsEnabled.value)
                    )
                }
                is Result.Error -> {
                    _mediaDetailsState.value = MediaDetailsState.Error(result.error.message)
                }
                is Result.Loading -> {
                    _mediaDetailsState.value = MediaDetailsState.Loading
                }
            }
        }
    }
    
    /**
     * Clear search results.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = SearchState.Idle
    }
    
    /**
     * Clear media details.
     */
    fun clearMediaDetails() {
        _mediaDetailsState.value = MediaDetailsState.Idle
    }
    
    /**
     * Retry search.
     */
    fun retrySearch() {
        val currentQuery = _searchQuery.value
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }
    
    /**
     * Retry loading media details.
     */
    fun retryMediaDetails() {
        when (val state = _mediaDetailsState.value) {
            is MediaDetailsState.Error -> {
                // Would need to store the ID to retry, for now just clear
                _mediaDetailsState.value = MediaDetailsState.Idle
            }
            else -> {}
        }
    }
}

/**
 * Search state.
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Success(val results: List<app.lusk.client.domain.model.SearchResult>) : SearchState()
    data class Error(val message: String) : SearchState()
}

/**
 * Media details state.
 */
sealed class MediaDetailsState {
    data object Idle : MediaDetailsState()
    data object Loading : MediaDetailsState()
    data class Success(val details: MediaDetails) : MediaDetailsState()
    data class Error(val message: String) : MediaDetailsState()
}

/**
 * Media details data class.
 */
data class MediaDetails(
    val title: String,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val genres: List<String>,
    val runtime: Int?,
    val status: String?,
    val isAvailable: Boolean,
    val isRequested: Boolean,
    val numberOfSeasons: Int = 0,
    val isPartiallyAvailable: Boolean = false,
    val isPartialRequestsEnabled: Boolean = false,
    val requestedSeasons: List<Int> = emptyList()
)

/**
 * Extension functions to convert domain models to MediaDetails.
 */
private fun Movie.toMediaDetails(partialRequestsEnabled: Boolean) = MediaDetails(
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    genres = emptyList(), // Not available in basic model
    runtime = null, // Not available in basic model
    status = mediaInfo?.status?.name,
    isAvailable = mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.AVAILABLE,
    isRequested = mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.PENDING ||
                  mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.PROCESSING,
    numberOfSeasons = 0,
    isPartiallyAvailable = false,
    isPartialRequestsEnabled = partialRequestsEnabled
)

private fun TvShow.toMediaDetails(partialRequestsEnabled: Boolean) = MediaDetails(
    title = name,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = firstAirDate,
    voteAverage = voteAverage,
    genres = emptyList(), // Not available in basic model
    runtime = null, // Not available in basic model
    status = mediaInfo?.status?.name,
    isAvailable = mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.AVAILABLE,
    isRequested = mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.PENDING ||
                  mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.PROCESSING,
    numberOfSeasons = numberOfSeasons,
    isPartiallyAvailable = mediaInfo?.status == app.lusk.client.domain.model.MediaStatus.PARTIALLY_AVAILABLE,
    isPartialRequestsEnabled = partialRequestsEnabled,
    requestedSeasons = mediaInfo?.requests?.flatMap { request -> 
        // Only include seasons from requests that are NOT declined
        if (request.status != app.lusk.client.domain.model.RequestStatus.DECLINED) {
            request.seasons ?: emptyList() 
        } else {
            emptyList()
        }
    } ?: emptyList()
)

/**
 * Extension function to convert Flow to StateFlow with initial value.
 */
private fun <T> kotlinx.coroutines.flow.Flow<T>.stateIn(
    scope: kotlinx.coroutines.CoroutineScope,
    initialValue: T
): StateFlow<T> {
    val stateFlow = MutableStateFlow(initialValue)
    scope.launch {
        collect { stateFlow.value = it }
    }
    return stateFlow.asStateFlow()
}
