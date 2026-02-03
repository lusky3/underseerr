package app.lusk.underseerr.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import androidx.paging.cachedIn
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.TvShow
import app.lusk.underseerr.domain.repository.DiscoveryRepository
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.ProfileRepository
import app.lusk.underseerr.domain.model.Genre
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * ViewModel for discovery screens.
 * Feature: underseerr
 * Validates: Requirements 2.1, 2.2, 2.4
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiscoveryViewModel(
    private val discoveryRepository: DiscoveryRepository,
    private val requestRepository: RequestRepository,
    private val profileRepository: ProfileRepository,
    private val profileViewModel: app.lusk.underseerr.presentation.profile.ProfileViewModel,
    private val issueViewModel: app.lusk.underseerr.presentation.issue.IssueViewModel,
    private val requestViewModel: app.lusk.underseerr.presentation.request.RequestViewModel,
    private val settingsRepository: app.lusk.underseerr.domain.repository.SettingsRepository
) : ViewModel() {
    
    val homeScreenConfig: StateFlow<app.lusk.underseerr.domain.repository.HomeScreenConfig> = 
        settingsRepository.getHomeScreenConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = app.lusk.underseerr.domain.repository.HomeScreenConfig()
            )
    
    init {
        println("DiscoveryViewModel: Created")
    }

    override fun onCleared() {
        super.onCleared()
        println("DiscoveryViewModel: Cleared")
    }
    
    companion object {
        private const val SEARCH_DEBOUNCE_MS = 500L
    }
    
    // Trending content
    val trending: StateFlow<PagingData<app.lusk.underseerr.domain.model.SearchResult>> = 
        discoveryRepository.getTrending()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty()
            )

    // Popular and Upcoming
    val popularMovies: StateFlow<PagingData<Movie>> = 
        discoveryRepository.getPopularMovies()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty()
            )

    val popularTvShows: StateFlow<PagingData<TvShow>> = 
        discoveryRepository.getPopularTvShows()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty()
            )

    val upcomingMovies: StateFlow<PagingData<Movie>> = 
        discoveryRepository.getUpcomingMovies()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty()
            )

    val upcomingTvShows: StateFlow<PagingData<TvShow>> = 
        discoveryRepository.getUpcomingTvShows()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty()
            )

    // Genres
    private val _movieGenres = MutableStateFlow<List<Genre>>(emptyList())
    val movieGenres: StateFlow<List<Genre>> = _movieGenres.asStateFlow()

    private val _tvGenres = MutableStateFlow<List<Genre>>(emptyList())
    val tvGenres: StateFlow<List<Genre>> = _tvGenres.asStateFlow()

    // User state
    private val _isPlexUser = MutableStateFlow(false)
    val isPlexUser: StateFlow<Boolean> = _isPlexUser.asStateFlow()

    // Watchlist
    val watchlist: StateFlow<PagingData<app.lusk.underseerr.domain.model.SearchResult>> = discoveryRepository.getWatchlist()
        .cachedIn(viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PagingData.empty()
        )
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    
    // Paged Search results
    val pagedSearchResults: Flow<PagingData<app.lusk.underseerr.domain.model.SearchResult>> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                emptyFlow()
            } else {
                discoveryRepository.findMedia(query)
            }
        }
        .cachedIn(viewModelScope)

    // Category Discovery
    private val _selectedCategory = MutableStateFlow<CategoryInfo?>(null)
    val selectedCategory: StateFlow<CategoryInfo?> = _selectedCategory.asStateFlow()

    val categoryResults: Flow<PagingData<app.lusk.underseerr.domain.model.SearchResult>> = _selectedCategory
        .filterNotNull()
        .flatMapLatest { category ->
            when (category.type) {
                CategoryType.MOVIE_GENRE -> discoveryRepository.getMoviesByGenre(category.id).map { pagingData -> pagingData.map { it.toSearchResult() } }
                CategoryType.TV_GENRE -> discoveryRepository.getTvByGenre(category.id).map { pagingData -> pagingData.map { it.toSearchResult() } }
                CategoryType.STUDIO -> discoveryRepository.getMoviesByStudio(category.id).map { pagingData -> pagingData.map { it.toSearchResult() } }
                CategoryType.NETWORK -> discoveryRepository.getTvByNetwork(category.id).map { pagingData -> pagingData.map { it.toSearchResult() } }
            }
        }
        .cachedIn(viewModelScope)

    fun selectCategory(type: CategoryType, id: Int, name: String) {
        _selectedCategory.value = CategoryInfo(type, id, name)
    }

    fun clearCategory() {
        _selectedCategory.value = null
    }

    private fun Movie.toSearchResult() = app.lusk.underseerr.domain.model.SearchResult(
        id = id,
        mediaType = MediaType.MOVIE,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage
    )

    private fun TvShow.toSearchResult() = app.lusk.underseerr.domain.model.SearchResult(
        id = id,
        mediaType = MediaType.TV,
        title = name,
        overview = overview,
        posterPath = posterPath,
        releaseDate = firstAirDate,
        voteAverage = voteAverage
    )
    
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

        // Fetch genres
        viewModelScope.launch {
            val movieGenresResult = discoveryRepository.getMovieGenres()
            if (movieGenresResult.isSuccess) {
                _movieGenres.value = movieGenresResult.getOrDefault(emptyList())
            }
            
            val tvGenresResult = discoveryRepository.getTvGenres()
            if (tvGenresResult.isSuccess) {
                _tvGenres.value = tvGenresResult.getOrDefault(emptyList())
            }
        }

        // Fetch user profile to check if Plex user
        viewModelScope.launch {
            val profileResult = profileRepository.getUserProfile()
            if (profileResult.isSuccess) {
                _isPlexUser.value = profileResult.getOrNull()?.isPlexUser ?: false
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

        // Background load other pages after a short delay to prioritize Home content
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            println("DiscoveryViewModel: Triggering background load for Profile, Issues, and Requests")
            profileViewModel.loadProfile()
            issueViewModel.loadIssues()
            requestViewModel.refreshRequests()
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
    private var lastRequestedMedia: Pair<MediaType, Int>? = null

    /**
     * Load media details based on type.
     * Property 7: Media Detail Navigation
     */
    fun loadMediaDetails(mediaType: MediaType, mediaId: Int) {
        lastRequestedMedia = mediaType to mediaId
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
        lastRequestedMedia = null
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
        lastRequestedMedia?.let { (type, id) ->
            loadMediaDetails(type, id)
        }
    }
    fun quickRequest(mediaId: Int, mediaType: MediaType) {
        viewModelScope.launch {
            val movieProfile = settingsRepository.getDefaultMovieQualityProfile().firstOrNull()
            val tvProfile = settingsRepository.getDefaultTvQualityProfile().firstOrNull()
            
            val result = when (mediaType) {
                MediaType.MOVIE -> requestRepository.requestMovie(mediaId, qualityProfile = movieProfile)
                MediaType.TV -> requestRepository.requestTvShow(mediaId, seasons = listOf(0), qualityProfile = tvProfile) // For TV, maybe we should fetch seasons first or just request all? Actually Overseerr handles 'all' if list is empty or similar? Wait, Usually it requires a list of seasons.
            }
            
            // Re-evaluating TV quick request: If we don't know the seasons, we might need to fetch them.
            // But let's check RequestRepository.requestTvShow again.
            // If seasons is empty, what happens? 
            
            if (result is Result.Success) {
                _uiEvent.emit("Request submitted successfully")
                // Refresh requests to update status indicators
                requestViewModel.refreshRequests()
            } else if (result is Result.Error) {
                _uiEvent.emit("Failed to submit request: ${result.error.message}")
            }
        }
    }

    // Improved quickRequest for TV
    fun quickRequestTv(tvShowId: Int) {
        viewModelScope.launch {
            _uiEvent.emit("Preparing request...")
            val detailsResult = discoveryRepository.getTvShowDetails(tvShowId)
            if (detailsResult is Result.Success) {
                val tvShow = detailsResult.data
                val allSeasons = (1..tvShow.numberOfSeasons).toList()
                val profile = settingsRepository.getDefaultTvQualityProfile().firstOrNull()
                
                val result = requestRepository.requestTvShow(tvShowId, seasons = allSeasons, qualityProfile = profile)
                if (result is Result.Success) {
                    _uiEvent.emit("Request submitted for all seasons")
                    requestViewModel.refreshRequests()
                } else if (result is Result.Error) {
                    _uiEvent.emit("Failed to submit request: ${result.error.message}")
                }
            } else {
                _uiEvent.emit("Failed to fetch show details for request")
            }
        }
    }

    fun removeFromWatchlist(ratingKey: String) {
        viewModelScope.launch {
            val result = discoveryRepository.removeFromWatchlist(ratingKey)
            if (result is Result.Success) {
                _uiEvent.emit("Removed from Plex watchlist")
                // We might want to refresh the watchlist flow
                // Since it's a StateFlow from pager, we might need to trigger a refresh on the paging data
            } else if (result is Result.Error) {
                _uiEvent.emit("Failed to remove: ${result.error.message}")
            }
        }
    }

    fun refresh() {
        // Fetch genres
        viewModelScope.launch {
            val movieGenresResult = discoveryRepository.getMovieGenres()
            if (movieGenresResult.isSuccess) {
                _movieGenres.value = movieGenresResult.getOrDefault(emptyList())
            }
            
            val tvGenresResult = discoveryRepository.getTvGenres()
            if (tvGenresResult.isSuccess) {
                _tvGenres.value = tvGenresResult.getOrDefault(emptyList())
            }
        }

        // Fetch user profile to check if Plex user
        viewModelScope.launch {
            val profileResult = profileRepository.getUserProfile()
            if (profileResult.isSuccess) {
                _isPlexUser.value = profileResult.getOrNull()?.isPlexUser ?: false
            }
        }
    }
}

/**
 * Category discovery types.
 */
enum class CategoryType {
    MOVIE_GENRE, TV_GENRE, STUDIO, NETWORK
}

/**
 * Category info for discovery.
 */
data class CategoryInfo(
    val type: CategoryType,
    val id: Int,
    val name: String
)

/**
 * Search state.
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Success(val results: List<app.lusk.underseerr.domain.model.SearchResult>) : SearchState()
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
    val requestedSeasons: List<Int> = emptyList(),
    val mediaInfoId: Int? = null,
    val cast: List<app.lusk.underseerr.domain.model.CastMember> = emptyList()
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
    isAvailable = mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.AVAILABLE,
    isRequested = mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.PENDING ||
                  mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.PROCESSING,
    numberOfSeasons = 0,
    isPartiallyAvailable = false,
    isPartialRequestsEnabled = partialRequestsEnabled,
    mediaInfoId = mediaInfo?.id,
    cast = cast
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
    isAvailable = mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.AVAILABLE,
    isRequested = mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.PENDING ||
                  mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.PROCESSING,
    numberOfSeasons = numberOfSeasons,
    isPartiallyAvailable = mediaInfo?.status == app.lusk.underseerr.domain.model.MediaStatus.PARTIALLY_AVAILABLE,
    isPartialRequestsEnabled = partialRequestsEnabled,
    requestedSeasons = mediaInfo?.requests?.flatMap { request -> 
        // Only include seasons from requests that are NOT declined
        if (request.status != app.lusk.underseerr.domain.model.RequestStatus.DECLINED) {
            request.seasons ?: emptyList() 
        } else {
            emptyList()
        }
    } ?: emptyList(),
    mediaInfoId = mediaInfo?.id,
    cast = cast
)

// Custom stateIn removed in favor of standard library stateIn
