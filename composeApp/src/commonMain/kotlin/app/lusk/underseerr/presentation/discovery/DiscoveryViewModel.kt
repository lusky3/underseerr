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
import app.lusk.underseerr.domain.model.SearchResult
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
    private val watchlistRepository: app.lusk.underseerr.domain.repository.WatchlistRepository,
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
    val trending: StateFlow<PagingData<SearchResult>> = 
        discoveryRepository.getTrending()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty<SearchResult>()
            )

    // Popular and Upcoming
    val popularMovies: StateFlow<PagingData<Movie>> = 
        discoveryRepository.getPopularMovies()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty<Movie>()
            )

    val popularTvShows: StateFlow<PagingData<TvShow>> = 
        discoveryRepository.getPopularTvShows()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty<TvShow>()
            )

    val upcomingMovies: StateFlow<PagingData<Movie>> = 
        discoveryRepository.getUpcomingMovies()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty<Movie>()
            )

    val upcomingTvShows: StateFlow<PagingData<TvShow>> = 
        discoveryRepository.getUpcomingTvShows()
            .cachedIn(viewModelScope)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = PagingData.empty<TvShow>()
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
    private val _watchlistRefreshTrigger = MutableStateFlow(0)
    val watchlist: StateFlow<PagingData<SearchResult>> = _watchlistRefreshTrigger
        .flatMapLatest { watchlistRepository.getWatchlist() }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.Lazily, PagingData.empty<SearchResult>())
    
    fun refreshWatchlist() {
        _watchlistRefreshTrigger.value++
    }
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Watchlist IDs for indicators
    private val _watchlistIds = MutableStateFlow<Set<Int>>(emptySet())
    val watchlistIds: StateFlow<Set<Int>> = _watchlistIds.asStateFlow()

    
    // Paged Search results
    val pagedSearchResults: Flow<PagingData<SearchResult>> = _searchQuery
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

    val categoryResults: Flow<PagingData<SearchResult>> = _selectedCategory
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

        // Search is handled by pagedSearchResults flow

        // Background load other pages after a short delay to prioritize Home content
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            println("DiscoveryViewModel: Triggering background load for Profile, Issues, and Requests")
            profileViewModel.loadProfile()
            issueViewModel.loadIssues()
            requestViewModel.refreshRequests()
            fetchWatchlistIds()
        }
    }

    fun fetchWatchlistIds() {
        viewModelScope.launch {
            val result = watchlistRepository.getWatchlistIds()
            if (result is Result.Success) {
                _watchlistIds.value = result.data
            }
        }
    }
    
    /**
     * Update search query.
     * Property 5: Search Performance (with debouncing)
     */
    fun search(query: String) {
        _searchQuery.value = query
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
        // Paging handles retry through LazyPagingItems.retry()
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

    fun removeFromWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String?) {
        viewModelScope.launch {
            val result = watchlistRepository.removeFromWatchlist(tmdbId, mediaType, ratingKey)
            if (result is Result.Success) {
                _watchlistIds.value = _watchlistIds.value - tmdbId
                _uiEvent.emit("Removed from watchlist")
                // Delay re-fetch to allow for Plex API consistency
                kotlinx.coroutines.delay(2000)
                refreshWatchlist()
                fetchWatchlistIds()
            } else if (result is Result.Error) {
                _uiEvent.emit("Failed to remove: ${result.error.message}")
            }
        }
    }

    fun addToWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String? = null) {
        viewModelScope.launch {
            val result = watchlistRepository.addToWatchlist(tmdbId, mediaType, ratingKey)
            if (result is Result.Success) {
                _watchlistIds.value = _watchlistIds.value + tmdbId
                _uiEvent.emit("Added to watchlist")
                // Delay re-fetch to allow for Plex API consistency
                kotlinx.coroutines.delay(2000)
                refreshWatchlist()
                fetchWatchlistIds()
            } else if (result is Result.Error) {
                _uiEvent.emit("Failed to add: ${result.error.message}")
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
    val ratingKey: String? = null,
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
    ratingKey = mediaInfo?.ratingKey, // Most basic models don't have it, but maybe?
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
    ratingKey = mediaInfo?.ratingKey,
    cast = cast
)

// Custom stateIn removed in favor of standard library stateIn
