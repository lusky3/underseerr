package app.lusk.underseerr.mock

import app.lusk.underseerr.data.remote.model.*

/**
 * Provides realistic mock response data for Overseerr API endpoints.
 */
object MockResponses {
    
    // Auth Responses
    fun authResponse() = ApiAuthResponse(
        apiKey = "test-api-key-12345",
        userId = 1
    )
    
    fun userProfile(userId: Int = 1) = ApiUserProfile(
        id = userId,
        email = "user$userId@example.com",
        displayName = "User $userId",
        avatar = "/avatar/$userId.jpg",
        requestCount = userId * 3,
        permissions = ApiPermissions(
            canRequest = true,
            canManageRequests = userId == 1,
            canViewRequests = true,
            isAdmin = userId == 1
        )
    )
    
    fun serverInfo() = ApiServerInfo(
        version = "1.33.2",
        initialized = true,
        applicationUrl = "http://localhost:5055"
    )
    
    // Discovery Responses
    fun searchResults(page: Int = 1, query: String = "") = ApiSearchResults(
        page = page,
        totalPages = 10,
        totalResults = 200,
        results = (1..20).map { index ->
            val id = (page - 1) * 20 + index
            ApiSearchResult(
                id = id,
                mediaType = if (id % 2 == 0) "movie" else "tv",
                title = if (id % 2 == 0) "Movie $id" else null,
                name = if (id % 2 != 0) "TV Show $id" else null,
                overview = "This is a great ${if (id % 2 == 0) "movie" else "TV show"} about $query",
                posterPath = "/poster/$id.jpg",
                releaseDate = if (id % 2 == 0) "2024-01-${(id % 28) + 1}" else null,
                firstAirDate = if (id % 2 != 0) "2024-01-${(id % 28) + 1}" else null,
                voteAverage = 7.0 + (id % 3)
            )
        }
    )
    
    fun movieSearchResults(page: Int = 1) = ApiSearchResults(
        page = page,
        totalPages = 10,
        totalResults = 200,
        results = (1..20).map { index ->
            val id = (page - 1) * 20 + index
            ApiSearchResult(
                id = id,
                mediaType = "movie",
                title = "Movie $id",
                name = null,
                overview = "An exciting action-packed movie with stunning visuals.",
                posterPath = "/poster/movie_$id.jpg",
                releaseDate = "2024-${(id % 12) + 1}-${(id % 28) + 1}",
                firstAirDate = null,
                voteAverage = 6.5 + (id % 4) * 0.5
            )
        }
    )
    
    fun tvShowSearchResults(page: Int = 1) = ApiSearchResults(
        page = page,
        totalPages = 10,
        totalResults = 200,
        results = (1..20).map { index ->
            val id = (page - 1) * 20 + index
            ApiSearchResult(
                id = id,
                mediaType = "tv",
                title = null,
                name = "TV Show $id",
                overview = "A thrilling drama series with complex characters.",
                posterPath = "/poster/tv_$id.jpg",
                releaseDate = null,
                firstAirDate = "2024-${(id % 12) + 1}-${(id % 28) + 1}",
                voteAverage = 7.0 + (id % 3) * 0.5
            )
        }
    )
    
    fun movieDetails(movieId: Int) = ApiMovie(
        id = movieId,
        title = "Movie $movieId",
        overview = "A detailed overview of movie $movieId with an engaging plot.",
        posterPath = "/poster/movie_$movieId.jpg",
        backdropPath = "/backdrop/movie_$movieId.jpg",
        releaseDate = "2024-06-15",
        voteAverage = 8.2,
        mediaInfo = ApiMediaInfo(
            status = if (movieId % 3 == 0) 5 else 1,
            requestId = if (movieId % 3 == 0) movieId else null,
            available = movieId % 5 == 0
        )
    )
    
    fun tvShowDetails(tvId: Int) = ApiTvShow(
        id = tvId,
        name = "TV Show $tvId",
        overview = "A detailed overview of TV show $tvId with multiple seasons.",
        posterPath = "/poster/tv_$tvId.jpg",
        backdropPath = "/backdrop/tv_$tvId.jpg",
        firstAirDate = "2024-01-01",
        voteAverage = 8.5,
        numberOfSeasons = 3,
        mediaInfo = ApiMediaInfo(
            status = if (tvId % 3 == 0) 5 else 1,
            requestId = if (tvId % 3 == 0) tvId else null,
            available = tvId % 5 == 0
        )
    )
    
    // Request Responses
    fun requestResponse(requestId: Int = 1) = ApiRequestResponse(
        id = requestId,
        status = 1,
        media = ApiMediaInfo(
            status = 1,
            requestId = requestId,
            available = false
        )
    )
    
    fun mediaRequest(requestId: Int) = ApiMediaRequest(
        id = requestId,
        mediaType = if (requestId % 2 == 0) "movie" else "tv",
        mediaId = requestId * 10,
        title = if (requestId % 2 == 0) "Movie Request $requestId" else "TV Show Request $requestId",
        posterPath = "/poster/request_$requestId.jpg",
        status = when (requestId % 4) {
            0 -> 1 // Pending
            1 -> 2 // Approved
            2 -> 3 // Declined
            else -> 5 // Available
        },
        createdAt = "2024-01-${(requestId % 28) + 1}T10:00:00.000Z",
        seasons = if (requestId % 2 != 0) listOf(1, 2) else null
    )
    
    fun requestsList(page: Int = 1) = ApiRequestsResponse(
        pageInfo = ApiPageInfo(
            pages = 5,
            pageSize = 20,
            results = 100,
            page = page
        ),
        results = (1..20).map { index ->
            val id = (page - 1) * 20 + index
            mediaRequest(id)
        }
    )
    
    fun requestStatus(requestId: Int) = mapOf(
        "status" to when (requestId % 4) {
            0 -> 1
            1 -> 2
            2 -> 3
            else -> 5
        }
    )
    
    fun qualityProfiles() = listOf(
        ApiQualityProfile(id = 1, name = "HD-1080p"),
        ApiQualityProfile(id = 2, name = "Ultra-HD"),
        ApiQualityProfile(id = 3, name = "SD"),
        ApiQualityProfile(id = 4, name = "4K")
    )
    
    fun rootFolders() = listOf(
        ApiRootFolder(id = 1, path = "/movies"),
        ApiRootFolder(id = 2, path = "/tv"),
        ApiRootFolder(id = 3, path = "/media/movies"),
        ApiRootFolder(id = 4, path = "/media/tv")
    )
    
    // User Responses
    fun userQuota() = ApiRequestQuota(
        movie = ApiQuotaInfo(
            limit = 10,
            remaining = 5,
            days = 7
        ),
        tv = ApiQuotaInfo(
            limit = 15,
            remaining = 7,
            days = 7
        )
    )
    
    fun userStatistics() = ApiUserStatistics(
        totalRequests = 25,
        approvedRequests = 15,
        declinedRequests = 3,
        pendingRequests = 5,
        availableRequests = 2
    )
}

// Additional model classes for mock responses
@kotlinx.serialization.Serializable
data class ApiRequestsResponse(
    val pageInfo: ApiPageInfo,
    val results: List<ApiMediaRequest>
)

@kotlinx.serialization.Serializable
data class ApiPageInfo(
    val pages: Int,
    val pageSize: Int,
    val results: Int,
    val page: Int
)

@kotlinx.serialization.Serializable
data class ApiQualityProfile(
    val id: Int,
    val name: String
)

@kotlinx.serialization.Serializable
data class ApiRootFolder(
    val id: Int,
    val path: String
)
