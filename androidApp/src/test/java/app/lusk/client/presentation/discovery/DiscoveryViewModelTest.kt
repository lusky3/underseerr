package app.lusk.client.presentation.discovery

import androidx.paging.PagingData
import app.lusk.client.domain.model.*
import app.lusk.client.domain.repository.DiscoveryRepository
import app.lusk.client.domain.repository.RequestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for DiscoveryViewModel.
 * Feature: overseerr-android-client
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
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
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
                    
                    coEvery { repository.searchMedia(any(), any()) } returns Result.Success(searchResults)
                    
                    // When - rapid queries
                    viewModel.search("a")
                    viewModel.search("ab")
                    viewModel.search("abc")
                    
                    // Advance time less than debounce period
                    advanceTimeBy(400)
                    
                    // Then - should not have called search yet
                    coVerify(exactly = 0) { repository.searchMedia(any(), any()) }
                    
                    // When - advance past debounce period
                    advanceTimeBy(200)
                    
                    // Then - should have called search once with final query
                    coVerify(exactly = 1) { repository.searchMedia("abc", any()) }
                }
            }
            
            it("should not search for blank queries") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    // When
                    viewModel.search("")
                    advanceTimeBy(600)
                    
                    // Then
                    viewModel.searchState.value.shouldBeInstanceOf<SearchState.Idle>()
                    coVerify(exactly = 0) { repository.searchMedia(any(), any()) }
                }
            }
        }
        
        describe("search functionality") {
            it("should update search state to loading when searching") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    // When
                    viewModel.search("test")
                    
                    // Then
                    viewModel.searchState.value.shouldBeInstanceOf<SearchState.Loading>()
                }
            }
            
            it("should update search state to success with results") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>()
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
                    
                    coEvery { repository.searchMedia("test", any()) } returns Result.Success(searchResults)
                    coEvery { repository.getTrendingMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getTrendingTvShows() } returns flowOf(PagingData.empty())
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    // When
                    viewModel.search("test")
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then
                    val state = viewModel.searchState.value
                    state.shouldBeInstanceOf<SearchState.Success>()
                    state.results.size shouldBe 2
                    state.results[0].title shouldBe "Test Movie"
                    state.results[1].title shouldBe "Test Show"
                }
            }
            
            it("should update search state to error on failure") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>()
                    val error = AppError.NetworkError("Network error")
                    
                    coEvery { repository.searchMedia("test", any()) } returns Result.Error(error)
                    coEvery { repository.getTrendingMovies() } returns flowOf(PagingData.empty())
                    coEvery { repository.getTrendingTvShows() } returns flowOf(PagingData.empty())
                    
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    // When
                    viewModel.search("test")
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    // Then
                    val state = viewModel.searchState.value
                    state.shouldBeInstanceOf<SearchState.Error>()
                    state.message shouldBe "Network error"
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
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
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
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
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
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
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
            it("should provide trending movies as paging data") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>()
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
                    
                    coEvery { repository.getTrendingMovies() } returns flowOf(pagingData)
                    coEvery { repository.getTrendingTvShows() } returns flowOf(PagingData.empty())
                    
                    // When
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    advanceTimeBy(100)
                    
                    // Then - should have trending movies flow
                    viewModel.trendingMovies.value shouldNotBe null
                }
            }
            
            it("should provide trending TV shows as paging data") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>()
                    val pagingData = PagingData.from(
                        listOf(
                            TvShow(
                                id = 1,
                                name = "Show 1",
                                overview = "Overview 1",
                                posterPath = "/test1.jpg",
                                backdropPath = null,
                                firstAirDate = "2024-01-01",
                                voteAverage = 8.0,
                                numberOfSeasons = 1,
                                mediaInfo = null
                            )
                        )
                    )
                    
                    coEvery { repository.getTrendingTvShows() } returns flowOf(pagingData)
                    coEvery { repository.getTrendingMovies() } returns flowOf(PagingData.empty())
                    
                    // When
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    coEvery { requestRepository.getPartialRequestsEnabled() } returns Result.success(false)
                    
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    advanceTimeBy(100)
                    
                    // Then - should have trending TV shows flow
                    viewModel.trendingTvShows.value shouldNotBe null
                }
            }
        }
        
        describe("clear operations") {
            it("should clear search results") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    viewModel.search("test")
                    advanceTimeBy(600)
                    
                    // When
                    viewModel.clearSearch()
                    
                    // Then
                    viewModel.searchState.value.shouldBeInstanceOf<SearchState.Idle>()
                    viewModel.searchQuery.value shouldBe ""
                }
            }
            
            it("should clear media details") {
                runTest(testDispatcher) {
                    // Given
                    val repository = mockk<DiscoveryRepository>(relaxed = true)
                    val requestRepository = mockk<RequestRepository>(relaxed = true)
                    val viewModel = DiscoveryViewModel(repository, requestRepository)
                    
                    // When
                    viewModel.clearMediaDetails()
                    
                    // Then
                    viewModel.mediaDetailsState.value.shouldBeInstanceOf<MediaDetailsState.Idle>()
                }
            }
        }
    }
})
