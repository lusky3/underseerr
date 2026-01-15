package app.lusk.client.data.repository

import app.lusk.client.data.remote.api.DiscoveryApiService
import app.lusk.client.data.remote.model.ApiMediaInfo
import app.lusk.client.data.remote.model.ApiMovie
import app.lusk.client.data.remote.model.ApiTvShow
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for media detail navigation.
 * Feature: overseerr-android-client, Property 7: Media Detail Navigation
 * Validates: Requirements 2.4
 */
class MediaDetailNavigationPropertyTest : StringSpec({
    
    "Property 7: Movie details should include all required information" {
        // Feature: overseerr-android-client, Property 7: Media Detail Navigation
        checkAll(100, Arb.int(1..100000), Arb.string(1..100), Arb.double(0.0..10.0)) { movieId, title, rating ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockMovie = ApiMovie(
                id = movieId,
                title = title,
                overview = "Detailed synopsis for $title",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                releaseDate = "2024-01-15",
                voteAverage = rating,
                mediaInfo = ApiMediaInfo(
                    status = 1,
                    requestId = null,
                    available = false
                )
            )
            
            coEvery { discoveryApiService.getMovieDetails(movieId) } returns mockMovie
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.getMovieDetails(movieId)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val movie = result.data
                
                // Should have synopsis
                movie.overview.shouldNotBeNull()
                movie.overview.isNotBlank() shouldBe true
                
                // Should have ratings
                movie.voteAverage shouldBe rating
                
                // Should have request status
                movie.mediaInfo.shouldNotBeNull()
                
                // Should preserve ID and title
                movie.id shouldBe movieId
                movie.title shouldBe title
            }
        }
    }
    
    "Property 7: TV show details should include all required information" {
        // Feature: overseerr-android-client, Property 7: Media Detail Navigation
        checkAll(100, Arb.int(1..100000), Arb.string(1..100), Arb.int(1..20)) { tvShowId, name, seasons ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockTvShow = ApiTvShow(
                id = tvShowId,
                name = name,
                overview = "Detailed synopsis for $name",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                firstAirDate = "2024-01-15",
                voteAverage = 8.5,
                numberOfSeasons = seasons,
                mediaInfo = ApiMediaInfo(
                    status = 1,
                    requestId = null,
                    available = false
                )
            )
            
            coEvery { discoveryApiService.getTvShowDetails(tvShowId) } returns mockTvShow
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.getTvShowDetails(tvShowId)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val tvShow = result.data
                
                // Should have synopsis
                tvShow.overview.shouldNotBeNull()
                tvShow.overview.isNotBlank() shouldBe true
                
                // Should have ratings
                tvShow.voteAverage shouldBe 8.5
                
                // Should have request status
                tvShow.mediaInfo.shouldNotBeNull()
                
                // Should have number of seasons
                tvShow.numberOfSeasons shouldBe seasons
                
                // Should preserve ID and name
                tvShow.id shouldBe tvShowId
                tvShow.name shouldBe name
            }
        }
    }
    
    "Property 7: Media details should indicate availability status" {
        // Feature: overseerr-android-client, Property 7: Media Detail Navigation
        checkAll(100, Arb.int(1..100000)) { movieId ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockMovie = ApiMovie(
                id = movieId,
                title = "Test Movie",
                overview = "Test overview",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                releaseDate = "2024-01-15",
                voteAverage = 7.5,
                mediaInfo = ApiMediaInfo(
                    status = 5, // Available
                    requestId = null,
                    available = true
                )
            )
            
            coEvery { discoveryApiService.getMovieDetails(movieId) } returns mockMovie
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.getMovieDetails(movieId)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val movie = result.data
                movie.mediaInfo.shouldNotBeNull()
                movie.mediaInfo!!.available shouldBe true
            }
        }
    }
    
    "Property 7: Media details should show request status when requested" {
        // Feature: overseerr-android-client, Property 7: Media Detail Navigation
        checkAll(100, Arb.int(1..100000), Arb.int(1..10000)) { movieId, requestId ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockMovie = ApiMovie(
                id = movieId,
                title = "Test Movie",
                overview = "Test overview",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                releaseDate = "2024-01-15",
                voteAverage = 7.5,
                mediaInfo = ApiMediaInfo(
                    status = 2, // Pending
                    requestId = requestId,
                    available = false
                )
            )
            
            coEvery { discoveryApiService.getMovieDetails(movieId) } returns mockMovie
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.getMovieDetails(movieId)
            }
            
            // Assert
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val movie = result.data
                movie.mediaInfo.shouldNotBeNull()
                movie.mediaInfo!!.requestId shouldBe requestId
                movie.mediaInfo!!.available shouldBe false
            }
        }
    }
    
    "Property 7: Media details should handle missing optional fields gracefully" {
        // Feature: overseerr-android-client, Property 7: Media Detail Navigation
        checkAll(100, Arb.int(1..100000)) { movieId ->
            // Arrange
            val discoveryApiService = mockk<DiscoveryApiService>()
            val mockMovie = ApiMovie(
                id = movieId,
                title = "Test Movie",
                overview = "Test overview",
                posterPath = null, // Missing poster
                backdropPath = null, // Missing backdrop
                releaseDate = null, // Missing release date
                voteAverage = 0.0, // No rating yet
                mediaInfo = null // No media info
            )
            
            coEvery { discoveryApiService.getMovieDetails(movieId) } returns mockMovie
            
            val repository = DiscoveryRepositoryImpl(discoveryApiService)
            
            // Act
            val result = runBlocking {
                repository.getMovieDetails(movieId)
            }
            
            // Assert - should still succeed even with missing optional fields
            result is Result.Success shouldBe true
            if (result is Result.Success) {
                val movie = result.data
                movie.id shouldBe movieId
                movie.title shouldBe "Test Movie"
            }
        }
    }
})
