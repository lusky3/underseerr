package app.lusk.client.data.paging

import androidx.paging.PagingSource
import app.lusk.client.data.remote.api.ApiMovieList
import app.lusk.client.data.remote.api.ApiTvShowList
import app.lusk.client.data.remote.api.DiscoveryApiService
import app.lusk.client.data.remote.model.ApiMovie
import app.lusk.client.data.remote.model.ApiTvShow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for pagination consistency.
 * Feature: overseerr-android-client, Property 8: Pagination Consistency
 * Validates: Requirements 2.5
 */
class PaginationConsistencyPropertyTest : StringSpec({
    
    "Property 8: Loading next page should increase total item count monotonically" {
        // Feature: overseerr-android-client, Property 8: Pagination Consistency
        checkAll(100, Arb.int(10..50)) { itemsPerPage ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            
            // Mock page 1
            val page1Movies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = index,
                    title = "Movie $index",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            // Mock page 2
            val page2Movies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = itemsPerPage + index,
                    title = "Movie ${itemsPerPage + index}",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            coEvery { discoveryApiService.getTrendingMovies(1) } returns ApiMovieList(
                page = 1,
                totalPages = 2,
                totalResults = itemsPerPage * 2,
                results = page1Movies
            )
            
            coEvery { discoveryApiService.getTrendingMovies(2) } returns ApiMovieList(
                page = 2,
                totalPages = 2,
                totalResults = itemsPerPage * 2,
                results = page2Movies
            )
            
            val pagingSource = TrendingMoviesPagingSource(discoveryApiService)
            
            // Act - Load page 1
            val page1Result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = 1,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Act - Load page 2
            val page2Result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Append(
                        key = 2,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Assert
            page1Result is PagingSource.LoadResult.Page shouldBe true
            page2Result is PagingSource.LoadResult.Page shouldBe true
            
            if (page1Result is PagingSource.LoadResult.Page && page2Result is PagingSource.LoadResult.Page) {
                val page1Count = page1Result.data.size
                val page2Count = page2Result.data.size
                val totalCount = page1Count + page2Count
                
                // Total count should increase monotonically
                totalCount shouldBeGreaterThan page1Count
                totalCount shouldBe (itemsPerPage * 2)
            }
        }
    }
    
    "Property 8: Pagination should not return duplicate items across pages" {
        // Feature: overseerr-android-client, Property 8: Pagination Consistency
        checkAll(100, Arb.int(5..20)) { itemsPerPage ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            
            val page1Movies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = index,
                    title = "Movie $index",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            val page2Movies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = itemsPerPage + index, // Different IDs
                    title = "Movie ${itemsPerPage + index}",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            coEvery { discoveryApiService.getTrendingMovies(1) } returns ApiMovieList(
                page = 1,
                totalPages = 2,
                totalResults = itemsPerPage * 2,
                results = page1Movies
            )
            
            coEvery { discoveryApiService.getTrendingMovies(2) } returns ApiMovieList(
                page = 2,
                totalPages = 2,
                totalResults = itemsPerPage * 2,
                results = page2Movies
            )
            
            val pagingSource = TrendingMoviesPagingSource(discoveryApiService)
            
            // Act
            val page1Result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = 1,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            val page2Result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Append(
                        key = 2,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Assert
            if (page1Result is PagingSource.LoadResult.Page && page2Result is PagingSource.LoadResult.Page) {
                val page1Ids = page1Result.data.map { it.id }.toSet()
                val page2Ids = page2Result.data.map { it.id }.toSet()
                
                // No overlap between pages
                val intersection = page1Ids.intersect(page2Ids)
                intersection.size shouldBe 0
            }
        }
    }
    
    "Property 8: Last page should have null nextKey" {
        // Feature: overseerr-android-client, Property 8: Pagination Consistency
        checkAll(100, Arb.int(5..20)) { itemsPerPage ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            
            val lastPageMovies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = index,
                    title = "Movie $index",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            coEvery { discoveryApiService.getTrendingMovies(1) } returns ApiMovieList(
                page = 1,
                totalPages = 1, // Only one page
                totalResults = itemsPerPage,
                results = lastPageMovies
            )
            
            val pagingSource = TrendingMoviesPagingSource(discoveryApiService)
            
            // Act
            val result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = 1,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Assert
            result is PagingSource.LoadResult.Page shouldBe true
            if (result is PagingSource.LoadResult.Page) {
                result.nextKey shouldBe null
            }
        }
    }
    
    "Property 8: First page should have null prevKey" {
        // Feature: overseerr-android-client, Property 8: Pagination Consistency
        checkAll(100, Arb.int(5..20)) { itemsPerPage ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            
            val firstPageMovies = List(itemsPerPage) { index ->
                ApiMovie(
                    id = index,
                    title = "Movie $index",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 7.5,
                    mediaInfo = null
                )
            }
            
            coEvery { discoveryApiService.getTrendingMovies(1) } returns ApiMovieList(
                page = 1,
                totalPages = 5,
                totalResults = itemsPerPage * 5,
                results = firstPageMovies
            )
            
            val pagingSource = TrendingMoviesPagingSource(discoveryApiService)
            
            // Act
            val result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = 1,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Assert
            result is PagingSource.LoadResult.Page shouldBe true
            if (result is PagingSource.LoadResult.Page) {
                result.prevKey shouldBe null
            }
        }
    }
    
    "Property 8: TV show pagination should work consistently like movies" {
        // Feature: overseerr-android-client, Property 8: Pagination Consistency
        checkAll(100, Arb.int(10..30)) { itemsPerPage ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            
            val tvShows = List(itemsPerPage) { index ->
                ApiTvShow(
                    id = index,
                    name = "TV Show $index",
                    overview = "Overview",
                    posterPath = null,
                    backdropPath = null,
                    firstAirDate = null,
                    voteAverage = 8.0,
                    numberOfSeasons = 5,
                    mediaInfo = null
                )
            }
            
            coEvery { discoveryApiService.getTrendingTvShows(1) } returns ApiTvShowList(
                page = 1,
                totalPages = 3,
                totalResults = itemsPerPage * 3,
                results = tvShows
            )
            
            val pagingSource = TrendingTvShowsPagingSource(discoveryApiService)
            
            // Act
            val result = runBlocking {
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = 1,
                        loadSize = itemsPerPage,
                        placeholdersEnabled = false
                    )
                )
            }
            
            // Assert
            result is PagingSource.LoadResult.Page shouldBe true
            if (result is PagingSource.LoadResult.Page) {
                result.data.size shouldBe itemsPerPage
                result.prevKey shouldBe null
                result.nextKey shouldNotBe null
                result.nextKey shouldBe 2
            }
        }
    }
})
