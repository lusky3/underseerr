package app.lusk.underseerr.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for domain model integrity.
 * Feature: underseerr, Property: Domain Model Integrity
 * Validates: Requirements 2.3, 3.1
 * 
 * These tests verify that domain models maintain their integrity through
 * creation and manipulation, ensuring data consistency.
 */
class DomainModelPropertyTest : StringSpec({

    "Property: Movie model preserves all properties" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(1..100),
            Arb.string(1..500),
            Arb.string(1..100).orNull(),
            Arb.string(1..100).orNull(),
            Arb.string(1..20).orNull(),
            Arb.double(0.0..10.0)
        ) { id, title, overview, posterPath, backdropPath, releaseDate, voteAverage ->
            val mediaInfo = MediaInfo(
                id = null,
                status = MediaStatus.AVAILABLE,
                requestId = null,
                available = true
            )
            
            val movie = Movie(
                id = id,
                title = title,
                overview = overview,
                posterPath = posterPath,
                backdropPath = backdropPath,
                releaseDate = releaseDate,
                voteAverage = voteAverage,
                mediaInfo = mediaInfo
            )
            
            // Verify all properties are preserved
            movie.id shouldBe id
            movie.title shouldBe title
            movie.overview shouldBe overview
            movie.posterPath shouldBe posterPath
            movie.backdropPath shouldBe backdropPath
            movie.releaseDate shouldBe releaseDate
            movie.voteAverage shouldBe voteAverage
            movie.mediaInfo shouldBe mediaInfo
        }
    }

    "Property: TvShow model preserves all properties" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(1..100),
            Arb.string(1..500),
            Arb.string(1..100).orNull(),
            Arb.string(1..100).orNull(),
            Arb.string(1..20).orNull(),
            Arb.double(0.0..10.0),
            Arb.int(1..20)
        ) { id, name, overview, posterPath, backdropPath, firstAirDate, voteAverage, numberOfSeasons ->
            val mediaInfo = MediaInfo(
                id = 1,
                status = MediaStatus.PENDING,
                requestId = 123,
                available = false
            )
            
            val tvShow = TvShow(
                id = id,
                name = name,
                overview = overview,
                posterPath = posterPath,
                backdropPath = backdropPath,
                firstAirDate = firstAirDate,
                voteAverage = voteAverage,
                numberOfSeasons = numberOfSeasons,
                mediaInfo = mediaInfo
            )
            
            // Verify all properties are preserved
            tvShow.id shouldBe id
            tvShow.name shouldBe name
            tvShow.overview shouldBe overview
            tvShow.posterPath shouldBe posterPath
            tvShow.backdropPath shouldBe backdropPath
            tvShow.firstAirDate shouldBe firstAirDate
            tvShow.voteAverage shouldBe voteAverage
            tvShow.numberOfSeasons shouldBe numberOfSeasons
            tvShow.mediaInfo shouldBe mediaInfo
        }
    }

    "Property: MediaRequest model preserves all properties" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.enum<MediaType>(),
            Arb.int(1..100000),
            Arb.string(1..100),
            Arb.string(1..100).orNull(),
            Arb.enum<RequestStatus>(),
            Arb.long(0..Long.MAX_VALUE),
            Arb.list(Arb.int(1..20), 0..10).orNull()
        ) { id, mediaType, mediaId, title, posterPath, status, requestedDate, seasons ->
            val request = MediaRequest(
                id = id,
                mediaType = mediaType,
                mediaId = mediaId,
                title = title,
                posterPath = posterPath,
                status = status,
                requestedDate = requestedDate,
                seasons = seasons
            )
            
            // Verify all properties are preserved
            request.id shouldBe id
            request.mediaType shouldBe mediaType
            request.mediaId shouldBe mediaId
            request.title shouldBe title
            request.posterPath shouldBe posterPath
            request.status shouldBe status
            request.requestedDate shouldBe requestedDate
            request.seasons shouldBe seasons
        }
    }

    "Property: UserProfile model preserves all properties" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(5..50),
            Arb.string(1..50),
            Arb.string(1..100).orNull(),
            Arb.int(0..1000),
            Arb.boolean()
        ) { id, email, displayName, avatar, requestCount, isPlexUser ->
            val permissions = Permissions(
                canRequest = true,
                canManageRequests = false,
                canViewRequests = true,
                isAdmin = false
            )
            
            val profile = UserProfile(
                id = id,
                email = email,
                displayName = displayName,
                avatar = avatar,
                requestCount = requestCount,
                permissions = permissions,
                isPlexUser = isPlexUser
            )
            
            // Verify all properties are preserved
            profile.id shouldBe id
            profile.email shouldBe email
            profile.displayName shouldBe displayName
            profile.avatar shouldBe avatar
            profile.requestCount shouldBe requestCount
            profile.permissions shouldBe permissions
            profile.isPlexUser shouldBe isPlexUser
        }
    }

    "Property: Data classes support copy with modifications" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(1..100)
        ) { id, title ->
            val original = Movie(
                id = id,
                title = title,
                overview = "Original overview",
                posterPath = null,
                backdropPath = null,
                releaseDate = null,
                voteAverage = 5.0,
                mediaInfo = null
            )
            
            val modified = original.copy(overview = "Modified overview")
            
            // Original should be unchanged
            original.overview shouldBe "Original overview"
            // Modified should have new value
            modified.overview shouldBe "Modified overview"
            // Other properties should be the same
            modified.id shouldBe original.id
            modified.title shouldBe original.title
        }
    }

    "Property: Enum values are distinct" {
        // MediaStatus enum values should all be distinct
        val mediaStatuses = MediaStatus.values().toSet()
        mediaStatuses.size shouldBe MediaStatus.values().size
        
        // RequestStatus enum values should all be distinct
        val requestStatuses = RequestStatus.values().toSet()
        requestStatuses.size shouldBe RequestStatus.values().size
        
        // MediaType enum values should all be distinct
        val mediaTypes = MediaType.values().toSet()
        mediaTypes.size shouldBe MediaType.values().size
    }

    "Property: Models with nullable fields handle null correctly" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(1..100)
        ) { id, title ->
            val movieWithNulls = Movie(
                id = id,
                title = title,
                overview = "",
                posterPath = null,
                backdropPath = null,
                releaseDate = null,
                voteAverage = 0.0,
                mediaInfo = null
            )
            
            movieWithNulls.posterPath shouldBe null
            movieWithNulls.backdropPath shouldBe null
            movieWithNulls.releaseDate shouldBe null
            movieWithNulls.mediaInfo shouldBe null
        }
    }

    "Property: Equality works correctly for data classes" {
        checkAll(100,
            Arb.int(1..100000),
            Arb.string(1..100)
        ) { id, title ->
            val movie1 = Movie(
                id = id,
                title = title,
                overview = "Test",
                posterPath = null,
                backdropPath = null,
                releaseDate = null,
                voteAverage = 5.0,
                mediaInfo = null
            )
            
            val movie2 = Movie(
                id = id,
                title = title,
                overview = "Test",
                posterPath = null,
                backdropPath = null,
                releaseDate = null,
                voteAverage = 5.0,
                mediaInfo = null
            )
            
            // Same values should be equal
            movie1 shouldBe movie2
            movie1.hashCode() shouldBe movie2.hashCode()
            
            // Different values should not be equal
            val movie3 = movie1.copy(title = "Different")
            movie1 shouldNotBe movie3
        }
    }
})
