package app.lusk.client.data.remote.api

import app.lusk.client.data.remote.model.ApiMediaRequest
import app.lusk.client.data.remote.model.ApiRequestBody
import app.lusk.client.data.remote.model.ApiRequestResponse
import app.lusk.client.data.remote.model.ApiSystemSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for media request endpoints.
 * Feature: overseerr-android-client
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
interface RequestApiService {
    
    @GET("/api/v1/settings/main")
    suspend fun getSystemSettings(): ApiSystemSettings
    
    /**
     * Submit a new media request.
     * 
     * @param body Request body containing media details
     * @return Response with created request information
     */
    @POST("/api/v1/request")
    suspend fun submitRequest(
        @Body body: ApiRequestBody
    ): ApiMediaRequest
    
    /**
     * Get all requests (filtered by permissions).
     * 
     * @param take Number of results to return (default: 20)
     * @param skip Number of results to skip for pagination (default: 0)
     * @param filter Filter by status: "all", "approved", "pending", "available", "declined" (default: "all")
     * @param sort Sort by "added" or "modified" (default: "added")
     * @param requestedBy Filter by user ID (optional)
     * @return Paginated list of requests
     */
    @GET("/api/v1/request")
    suspend fun getRequests(
        @Query("take") take: Int = 20,
        @Query("skip") skip: Int = 0,
        @Query("filter") filter: String = "all",
        @Query("sort") sort: String = "added",
        @Query("requestedBy") requestedBy: Int? = null
    ): RequestsResponse
    
    /**
     * Get a specific request by ID.
     * 
     * @param requestId The ID of the request
     * @return Request details
     */
    @GET("/api/v1/request/{requestId}")
    suspend fun getRequest(
        @Path("requestId") requestId: Int
    ): ApiMediaRequest
    
    /**
     * Delete/cancel a request.
     * 
     * @param requestId The ID of the request to delete
     */
    @DELETE("/api/v1/request/{requestId}")
    suspend fun deleteRequest(
        @Path("requestId") requestId: Int
    )
    
    /**
     * Get requests for a specific user.
     * 
     * @param userId The ID of the user
     * @param take Number of results to return (default: 20)
     * @param skip Number of results to skip for pagination (default: 0)
     * @return Paginated list of user's requests
     */
    @GET("/api/v1/user/{userId}/requests")
    suspend fun getUserRequests(
        @Path("userId") userId: Int,
        @Query("take") take: Int = 20,
        @Query("skip") skip: Int = 0
    ): RequestsResponse
    
    /**
     * Request a movie.
     */
    @POST("/api/v1/request")
    suspend fun requestMovie(
        @Query("mediaId") movieId: Int,
        @Query("qualityProfile") qualityProfile: Int? = null,
        @Query("rootFolder") rootFolder: String? = null
    ): ApiMediaRequest
    
    /**
     * Request a TV show with seasons.
     */
    @POST("/api/v1/request")
    suspend fun requestTvShow(
        @Query("mediaId") tvShowId: Int,
        @Query("seasons") seasons: List<Int>,
        @Query("qualityProfile") qualityProfile: Int? = null,
        @Query("rootFolder") rootFolder: String? = null
    ): ApiMediaRequest
    
    /**
     * Cancel a request.
     */
    @DELETE("/api/v1/request/{requestId}")
    suspend fun cancelRequest(
        @Path("requestId") requestId: Int
    )
    
    /**
     * Get request status.
     */
    @GET("/api/v1/request/{requestId}/status")
    suspend fun getRequestStatus(
        @Path("requestId") requestId: Int
    ): ApiRequestStatus
    
    /**
     * Get quality profiles.
     */
    @GET("/api/v1/service/radarr")
    suspend fun getRadarrServers(): List<ApiMediaServer>

    @GET("/api/v1/service/sonarr")
    suspend fun getSonarrServers(): List<ApiMediaServer>
    
    @GET("/api/v1/service/radarr/{id}")
    suspend fun getRadarrService(@Path("id") id: Int): ApiServiceSettings

    @GET("/api/v1/service/sonarr/{id}")
    suspend fun getSonarrService(@Path("id") id: Int): ApiServiceSettings
}

@Serializable
data class ApiMediaServer(
    val id: Int,
    val name: String,
    val isDefault: Boolean = false
)

@Serializable
data class ApiServiceSettings(
    val profiles: List<ApiQualityProfile>,
    val rootFolders: List<ApiRootFolder>
)

/**
 * API request status response.
 */
@kotlinx.serialization.Serializable
data class ApiRequestStatus(
    @kotlinx.serialization.SerialName("status")
    val status: String
)

/**
 * API quality profile.
 */
@kotlinx.serialization.Serializable
data class ApiQualityProfile(
    @kotlinx.serialization.SerialName("id")
    val id: Int,
    @kotlinx.serialization.SerialName("name")
    val name: String
)

/**
 * API root folder.
 */
@kotlinx.serialization.Serializable
data class ApiRootFolder(
    @kotlinx.serialization.SerialName("id")
    val id: String,
    @kotlinx.serialization.SerialName("path")
    val path: String
)

/**
 * Response for paginated requests list.
 */
@kotlinx.serialization.Serializable
data class RequestsResponse(
    @kotlinx.serialization.SerialName("pageInfo")
    val pageInfo: PageInfo,
    @kotlinx.serialization.SerialName("results")
    val results: List<ApiMediaRequest>
)

/**
 * Pagination information.
 */
@kotlinx.serialization.Serializable
data class PageInfo(
    @kotlinx.serialization.SerialName("pages")
    val pages: Int,
    @kotlinx.serialization.SerialName("pageSize")
    val pageSize: Int,
    @kotlinx.serialization.SerialName("results")
    val results: Int,
    @kotlinx.serialization.SerialName("page")
    val page: Int
)
