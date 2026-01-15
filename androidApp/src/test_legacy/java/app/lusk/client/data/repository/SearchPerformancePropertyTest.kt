package app.lusk.client.data.repository

import app.lusk.client.data.remote.api.ApiSearchResults
import app.lusk.client.data.remote.api.DiscoveryApiService
import app.lusk.client.data.remote.model.ApiSearchResult
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * Property-based tests for search performance.
 * Feature: overseerr-android-client, Property 5: Search Performance
 * Validates: Requirements 2.2
 */
class SearchPerformancePropertyTest : StringSpec({
    
    "Property 5: Search should return results within 2 seconds" {
        // Feature: overseerr-android-client, Property 5: Search Performance
        checkAll(100, Arb.string(1..50)) { query ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = 5,
                results = List(5) { index ->
                    ApiSearchResult(
                        id = index,
                        mediaType = "movie",
                        title = "Movie $index",
                        name = null,
                        overview = "Overview",
                        posterPath = null,
                        backdropPath = null,
                        releaseDate = null,
                        firstAirDate = null,
                        voteAverage = 7.5,
                        mediaInfo = null
                    )
                }
            )
            
            coEvery { discoveryApiService.searchMedia(query, any()) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act & Assert
            val timeMillis = measureTimeMillis {
                runBlocking {
                    repository.searchMedia(query, 1)
                }
            }
            
            // Should complete within 2000ms (2 seconds)
            timeMillis shouldBeLessThan 2000L
        }
    }
    
    "Property 5: Search with empty query should still perform within timeout" {
        // Feature: overseerr-android-client, Property 5: Search Performance
        checkAll(100, Arb.int(1..10)) { page ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = page,
                totalPages = 10,
                totalResults = 100,
                results = emptyList()
            )
            
            coEvery { discoveryApiService.searchMedia("", page) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act & Assert
            val timeMillis = measureTimeMillis {
                runBlocking {
                    repository.searchMedia("", page)
                }
            }
            
            timeMillis shouldBeLessThan 2000L
        }
    }
    
    "Property 5: Search should return success result when API responds" {
        // Feature: overseerr-android-client, Property 5: Search Performance
        checkAll(100, Arb.string(1..50), Arb.int(1..100)) { query, resultCount ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = resultCount,
                results = List(resultCount.coerceAtMost(20)) { index ->
                    ApiSearchResult(
                        id = index,
                        mediaType = if (index % 2 == 0) "movie" else "tv",
                        title = if (index % 2 == 0) "Movie $index" else null,
                        name = if (index % 2 == 1) "TV Show $index" else null,
                        overview = "Overview $index",
                        posterPath = "/poster$index.jpg",
                        backdropPath = "/backdrop$index.jpg",
                        releaseDate = if (index % 2 == 0) "2024-01-01" else null,
                        firstAirDate = if (index % 2 == 1) "2024-01-01" else null,
                        voteAverage = 7.5,
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
                result.data.totalResults shouldBe resultCount
            }
        }
    }
    
    "Property 5: Multiple consecutive searches should all complete within timeout" {
        // Feature: overseerr-android-client, Property 5: Search Performance
        checkAll(50, Arb.string(1..30)) { query ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockResults = ApiSearchResults(
                page = 1,
                totalPages = 1,
                totalResults = 10,
                results = List(10) { index ->
                    ApiSearchResult(
                        id = index,
                        mediaType = "movie",
                        title = "Movie $index",
                        name = null,
                        overview = "Overview",
                        posterPath = null,
                        backdropPath = null,
                        releaseDate = null,
                        firstAirDate = null,
                        voteAverage = 7.5,
                        mediaInfo = null
                    )
                }
            )
            
            coEvery { discoveryApiService.searchMedia(any(), any()) } returns mockResults
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act & Assert - perform 3 consecutive searches
            repeat(3) {
                val timeMillis = measureTimeMillis {
                    runBlocking {
                        repository.searchMedia(query, 1)
                    }
                }
                timeMillis shouldBeLessThan 2000L
            }
        }
    }
})
