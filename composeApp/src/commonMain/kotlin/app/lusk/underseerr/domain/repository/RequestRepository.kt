package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.RequestStatus
import app.lusk.underseerr.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for media request operations.
 * Feature: underseerr
 * Validates: Requirements 3.1, 3.2, 4.1, 4.4
 */
interface RequestRepository {
    
    /**
     * Request a movie.
     * Property 9: Request Submission Completeness
     */
    suspend fun requestMovie(
        movieId: Int,
        qualityProfile: Int? = null,
        rootFolder: String? = null
    ): Result<MediaRequest>
    
    /**
     * Request a TV show with specific seasons.
     * Property 9: Request Submission Completeness
     */
    suspend fun requestTvShow(
        tvShowId: Int,
        seasons: List<Int>,
        qualityProfile: Int? = null,
        rootFolder: String? = null
    ): Result<MediaRequest>
    
    /**
     * Get all user requests.
     * Property 13: Request List Completeness
     */
    fun getUserRequests(): Flow<List<MediaRequest>>
    
    /**
     * Get requests filtered by status.
     * Property 14: Request Grouping Correctness
     */
    fun getRequestsByStatus(status: RequestStatus): Flow<List<MediaRequest>>
    
    /**
     * Cancel a pending request.
     * Property 16: Permission-Based Cancellation
     */
    suspend fun cancelRequest(requestId: Int): Result<Unit>
    
    /**
     * Get request status.
     * Property 17: Request Status Updates
     */
    suspend fun getRequestStatus(requestId: Int): Result<RequestStatus>
    
    /**
     * Refresh requests from server.
     * Property 18: Pull-to-Refresh Data Freshness
     * @return Result containing Pair(itemsFetchedInThisPage, totalItemsOnServer)
     */
    suspend fun refreshRequests(page: Int = 1, pageSize: Int = 10): Result<Pair<Int, Int>>
    
    /**
     * Check if media is already requested.
     * Property 11: Duplicate Request Prevention
     */
    suspend fun isMediaRequested(mediaId: Int): Result<Boolean>
    
    /**
     * Get available quality profiles.
     * Property 12: Advanced Options Availability
     */
    suspend fun getQualityProfiles(isMovie: Boolean = true): Result<List<QualityProfile>>
    
    /**
     * Get available root folders.
     * Property 12: Advanced Options Availability
     */
    suspend fun getRootFolders(isMovie: Boolean = true): Result<List<RootFolder>>
    
    suspend fun getPartialRequestsEnabled(): Result<Boolean>
    
    suspend fun approveRequest(requestId: Int): Result<MediaRequest>
    
    suspend fun declineRequest(requestId: Int): Result<Unit>
    
    suspend fun repairRequest(request: MediaRequest): Result<MediaRequest>
}

/**
 * Quality profile for media requests.
 */
data class QualityProfile(
    val id: Int,
    val name: String
)

/**
 * Root folder for media storage.
 */
data class RootFolder(
    val id: String,
    val path: String
)
