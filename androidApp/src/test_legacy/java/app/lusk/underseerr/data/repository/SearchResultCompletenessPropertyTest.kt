package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.remote.api.ApiSearchResults
import app.lusk.underseerr.data.remote.api.DiscoveryApiService
import app.lusk.underseerr.data.remote.model.ApiSearchResult
import app.lusk.underseerr.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for search result completeness.
 * Feature: underseerr, Property 6: Search Result Completeness
 * Validates: Requirements 2.3
 */
class SearchResultCompletenessPropertyTest : StringSpec({
    
    "Property 6: All search results should include required fields" {
        // Feature: underseerr, Property 6: Search Result Completeness
        checkAll(100, Arb.string(1..50), Arb.int(1..20)) { query, resultCount ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = resultCount,
                results = List(resultCount) { index ->
                    ApiSearchResult(
                        id = index,
                        mediaType = if (index % 2 == 0) "movie" else "tv",
                        title = if (index % 2 == 0) "Movie $index" else null,
                        name = if (index % 2 == 1) "TV Show $index" else null,
                        overview = "Overview for item $index",
                        posterPath = "/poster$index.jpg",
                        backdropPath = "/backdrop$index.jpg",
                        releaseDate = if (index % 2 == 0) "2024-01-01" else null,
                        firstAirDate = if (index % 2 == 1) "2024-01-01" else null,
                        voteAverage = 7.5 + (index % 3),
                        mediaInfo = null
                    )
                }
            )
            
            coEvery { discoveryApiService.searchMedia(query, 1) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.searchMedia(query, 1)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val searchResults = result.data
                
                // All results should have required fields
                searchResults.results.forEach { searchResult ->
                    // Should have title (for movies) or name (for TV shows)
                    (searchResult.title != null || searchResult.name != null) shouldBe true
                    
                    // Should have poster path (or placeholder will be used in UI)
                    searchResult.posterPath.shouldNotBeNull()
                    
                    // Should have release year (from releaseDate or firstAirDate)
                    (searchResult.releaseDate != null || searchResult.firstAirDate != null) shouldBe true
                    
                    // Should have media info (availability status)
                    searchResult.mediaInfo.shouldNotBeNull()
                }
            }
        }
    }
    
    "Property 6: Search results should preserve all data from API" {
        // Feature: underseerr, Property 6: Search Result Completeness
        checkAll(100, Arb.string(1..50)) { query ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val testId = 12345
            val testTitle = "Test Movie"
            val testOverview = "Test Overview"
            val testPosterPath = "/test_poster.jpg"
            val testReleaseDate = "2024-06-15"
            val testVoteAverage = 8.5
            
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = 1,
                results = listOf(
                    ApiSearchResult(
                        id = testId,
                        mediaType = "movie",
                        title = testTitle,
                        name = null,
                        overview = testOverview,
                        posterPath = testPosterPath,
                        backdropPath = "/test_backdrop.jpg",
                        releaseDate = testReleaseDate,
                        firstAirDate = null,
                        voteAverage = testVoteAverage,
                        mediaInfo = null
                    )
                )
            )
            
            coEvery { discoveryApiService.searchMedia(query, 1) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.searchMedia(query, 1)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val searchResult = result.data.results.first()
                searchResult.id shouldBe testId
                searchResult.title shouldBe testTitle
                searchResult.overview shouldBe testOverview
                searchResult.posterPath shouldBe testPosterPath
                searchResult.releaseDate shouldBe testReleaseDate
                searchResult.voteAverage shouldBe testVoteAverage
            }
        }
    }
    
    "Property 6: Empty search results should still be valid" {
        // Feature: underseerr, Property 6: Search Result Completeness
        checkAll(100, Arb.string(1..50)) { query ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 0,
                totalResults = 0,
                results = emptyList()
            )
            
            coEvery { discoveryApiService.searchMedia(query, 1) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.searchMedia(query, 1)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                result.data.results.size shouldBe 0
                result.data.totalResults shouldBe 0
            }
        }
    }
    
    "Property 6: Search results should include both movies and TV shows" {
        // Feature: underseerr, Property 6: Search Result Completeness
        checkAll(100, Arb.string(1..50)) { query ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = 4,
                results = listOf(
                    ApiSearchResult(
                        id = 1,
                        mediaType = "movie",
                        title = "Movie 1",
                        name = null,
                        overview = "Movie overview",
                        posterPath = "/movie1.jpg",
                        backdropPath = null,
                        releaseDate = "2024-01-01",
                        firstAirDate = null,
                        voteAverage = 7.5,
                        mediaInfo = null
                    ),
                    ApiSearchResult(
                        id = 2,
                        mediaType = "tv",
                        title = null,
                        name = "TV Show 1",
                        overview = "TV overview",
                        posterPath = "/tv1.jpg",
                        backdropPath = null,
                        releaseDate = null,
                        firstAirDate = "2024-01-01",
                        voteAverage = 8.0,
                        mediaInfo = null
                    )
                )
            )
            
            coEvery { discoveryApiService.searchMedia(query, 1) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.searchMedia(query, 1)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val results = result.data.results
                results.size shouldBe 2
                
                // Should have at least one movie
                results.any { it.mediaType == "movie" } shouldBe true
                
                // Should have at least one TV show
                results.any { it.mediaType == "tv" } shouldBe true
            }
        }
    }
})
