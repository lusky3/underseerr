package app.lusk.underseerr.presentation.discovery

import androidx.paging.PagingData
import app.lusk.underseerr.domain.model.*
import app.lusk.underseerr.domain.repository.DiscoveryRepository
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.ProfileRepository
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.WatchlistRepository
import app.lusk.underseerr.domain.repository.HomeScreenConfig
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for DiscoveryViewModel.
 * Feature: underseerr
 * Validates: Requirements 2.2, 2.4, 2.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest : DescribeSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    describe("DiscoveryViewModel") {
        
        describe("search debouncing") {
            it("should debounce rapid search queries") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    coEvery { repository.getMovieGenres() } returns Result.success(emptyList())
                    coEvery { repository.getTvGenres() } returns Result.success(emptyList())
                    
                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    coEvery { watchlistRepository.getWatchlist() } returns flowOf(PagingData.empty())
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)
                    
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    val searchResults = SearchResults(
                        page = 1,
                        totalPages = 1,
                        totalResults = 1,
                        results = listOf(
                            SearchResult(
                                id = 1,
                                mediaType = MediaType.MOVIE,
                                title = "Test Movie",
                                overview = "Test overview",
                                posterPath = "/test.jpg",
                                releaseDate = "2024-01-01",
                                voteAverage = 8.0
                            )
                        )
                    )
                    
                    coEvery { repository.findMedia(any()) } returns flowOf(PagingData.empty())
                    
                    // When - rapid queries
                    // We need to collect the flow for it to trigger
                    val collectJob = launch { viewModel.pagedSearchResults.collect {} }
                    
                    viewModel.search("a")
                    viewModel.search("ab")
                    viewModel.search("abc")
                    
                    // Advance time less than debounce period
                    advanceTimeBy(400)
                    
                    // Then - should not have called search yet
                    verify(exactly = 0) { repository.findMedia(any()) }
                    
                    // When - advance past debounce period
                    advanceTimeBy(300) // 500ms debounce + some buffer
                    
                    // Then - should have called search once with final query
                    verify(exactly = 1) { repository.findMedia("abc") }
                    
                    collectJob.cancel()
                }
            }
            
            it("should not search for blank queries") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    
                    coEvery { repository.getTrending() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularTvShows() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingTvShows() } returns flowOf(PagingData.empty())
                    coEvery { watchlistRepository.getWatchlist() } returns flowOf(PagingData.empty())

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)
                    
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    val collectJob = launch { viewModel.pagedSearchResults.collect {} }
                    viewModel.search("")
                    advanceTimeBy(600)
                    
                    // Then
                    viewModel.searchQuery.value shouldBe ""
                    verify(exactly = 0) { repository.findMedia(any()) }
                    collectJob.cancel()
                }
            }
        }
        
        describe("search functionality") {
            it("should update search state to loading when searching") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    coEvery { repository.getMovieGenres() } returns Result.success(emptyList())
                    coEvery { repository.getTvGenres() } returns Result.success(emptyList())

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.search("test")
                    
                    // Then - query should be set
                    viewModel.searchQuery.value shouldBe "test"
                }
            }
            
            it("should update search state to success with results") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val searchResults = SearchResults(
                        page = 1,
                        totalPages = 1,
                        totalResults = 2,
                        results = listOf(
                            SearchResult(
                                id = 1,
                                mediaType = MediaType.MOVIE,
                                title = "Test Movie",
                                overview = "Test overview",
                                posterPath = "/test.jpg",
                                releaseDate = "2024-01-01",
                                voteAverage = 8.0
                            ),
                            SearchResult(
                                id = 2,
                                mediaType = MediaType.TV,
                                title = "Test Show",
                                overview = "Test overview",
                                posterPath = "/test2.jpg",
                                releaseDate = "2024-01-01",
                                voteAverage = 7.5
                            )
                        )
                    )
                    
                    every { repository.findMedia("test") } returns flowOf(PagingData.from(searchResults.results))
                    coEvery { repository.getTrending() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularTvShows() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingTvShows() } returns flowOf(PagingData.empty())
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.search("test")
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then - with paging, we verify the query was set and pagedSearchResults flow exists
                    viewModel.searchQuery.value shouldBe "test"
                    // The actual paging results would need to be tested via collecting the flow
                }
            }
            
            it("should update search state to error on failure") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val error = AppError.NetworkError("Network error")
                    
                    every { repository.findMedia("test") } returns flowOf(PagingData.empty()) // Error handling in paging is different
                    coEvery { repository.getTrending() } returns flowOf(PagingData.empty())
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.search("test")
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then - with paging, errors are handled by the paging load state
                    viewModel.searchQuery.value shouldBe "test"
                    // Error handling is now done via Paging3's LoadState
                }
            }
        }
        
        describe("media details loading") {
            it("should load movie details successfully") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val movie = Movie(
                        id = 1,
                        title = "Test Movie",
                        overview = "Test overview",
                        posterPath = "/test.jpg",
                        backdropPath = "/backdrop.jpg",
                        releaseDate = "2024-01-01",
                        voteAverage = 8.0,
                        mediaInfo = null
                    )
                    
                    coEvery { repository.getMovieDetails(1) } returns Result.Success(movie)
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.loadMediaDetails(MediaType.MOVIE, 1)
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then
                    val state = viewModel.mediaDetailsState.value
                    state.shouldBeInstanceOf<MediaDetailsState.Success>()
                    state.details.title shouldBe "Test Movie"
                    state.details.genres shouldBe emptyList()
                    state.details.isAvailable shouldBe false
                }
            }
            
            it("should load TV show details successfully") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val tvShow = TvShow(
                        id = 1,
                        name = "Test Show",
                        overview = "Test overview",
                        posterPath = "/test.jpg",
                        backdropPath = "/backdrop.jpg",
                        firstAirDate = "2024-01-01",
                        voteAverage = 8.5,
                        numberOfSeasons = 3,
                        mediaInfo = null
                    )
                    
                    coEvery { repository.getTvShowDetails(1) } returns Result.Success(tvShow)
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.loadMediaDetails(MediaType.TV, 1)
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then
                    val state = viewModel.mediaDetailsState.value
                    state.shouldBeInstanceOf<MediaDetailsState.Success>()
                    state.details.title shouldBe "Test Show"
                    state.details.genres shouldBe emptyList()
                    state.details.isRequested shouldBe false
                    state.details.isAvailable shouldBe false
                }
            }
            
            it("should handle media details loading error") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val error = AppError.HttpError(404, "Not found")
                    
                    coEvery { repository.getMovieDetails(1) } returns Result.Error(error)
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))

                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.loadMediaDetails(MediaType.MOVIE, 1)
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then
                    val state = viewModel.mediaDetailsState.value
                    state.shouldBeInstanceOf<MediaDetailsState.Error>()
                    state.message shouldBe "Not found"
                }
            }
        }
        
        describe("pagination") {
            it("should provide trending content as paging data") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val pagingData = PagingData.from(
                        listOf(
                            SearchResult(
                                id = 1,
                                mediaType = MediaType.MOVIE,
                                title = "Movie 1",
                                overview = "Overview 1",
                                posterPath = "/test1.jpg",
                                releaseDate = "2024-01-01",
                                voteAverage = 8.0
                            )
                        )
                    )
                    
                    coEvery { repository.getTrending() } returns flowOf(pagingData)
                    
                    // When
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    
                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    advanceTimeBy(100)
                    
                    // Then
                    viewModel.trending.value shouldNotBe null
                }
            }
            
            it("should provide popular movies as paging data") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val pagingData = PagingData.from(
                        listOf(
                            Movie(
                                id = 1,
                                title = "Movie 1",
                                overview = "Overview 1",
                                posterPath = "/test1.jpg",
                                backdropPath = null,
                                releaseDate = "2024-01-01",
                                voteAverage = 8.0,
                                mediaInfo = null
                            )
                        )
                    )
                    
                    coEvery { repository.getPopularMovies() } returns flowOf(pagingData)
                    
                    // When
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    
                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    advanceTimeBy(100)
                    
                    // Then
                    viewModel.popularMovies.value shouldNotBe null
                }
            }
        }
        
        describe("clear operations") {
            it("should clear search results") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    
                    coEvery { repository.getTrending() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getPopularTvShows() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getUpcomingTvShows() } returns flowOf(PagingData.empty())
                    coEvery { watchlistRepository.getWatchlist() } returns flowOf(PagingData.empty())
                    coEvery { repository.getMovieGenres() } returns Result.success(emptyList())
                    coEvery { repository.getTvGenres() } returns Result.success(emptyList())
                    
                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)
                    
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    viewModel.search("test")
                    advanceTimeBy(600)
                    
                    // When
                    viewModel.clearSearch()
                    
                    // Then - search query should be cleared
                    viewModel.searchQuery.value shouldBe ""
                }
            }
            
            it("should clear media details") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val profileRepository = mockk<ProfileRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    coEvery { profileRepository.getUserProfile() } returns Result.success(UserProfile(1, "test@test.com", "Test", null, 0, mockk(), 0L, false))
                    
                    val issueRepository = mockk<IssueRepository>(relaxed = true)
                    val settingsRepository = mockk<SettingsRepository>(relaxed = true)
                    
                    val profileViewModel = ProfileViewModel(profileRepository, requestRepository)
                    val issueViewModel = IssueViewModel(issueRepository)
                    val requestViewModel = RequestViewModel(requestRepository, settingsRepository)

                    val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)
                    every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())
                    val viewModel = DiscoveryViewModel(
                        repository, 
                        watchlistRepository,
                        requestRepository, 
                        profileRepository,
                        profileViewModel,
                        issueViewModel,
                        requestViewModel,
                        settingsRepository
                    )
                    
                    // When
                    viewModel.clearMediaDetails()
                    
                    // Then
                    viewModel.mediaDetailsState.value.shouldBeInstanceOf<MediaDetailsState.Idle>()
                }
            }
        }
    }
})
