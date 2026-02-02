package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.model.toMediaRequest
import app.lusk.underseerr.data.remote.api.RequestKtorService
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.RequestStatus
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.QualityProfile
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.RootFolder
import app.lusk.underseerr.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock as KClock

/**
 * Implementation of RequestRepository.
 * Feature: underseerr
 * Validates: Requirements 3.1, 3.2, 4.1, 4.4
 */
class RequestRepositoryImpl(
    private val requestKtorService: RequestKtorService,
    private val mediaRequestDao: MediaRequestDao,
    private val offlineRequestDao: app.lusk.underseerr.data.local.dao.OfflineRequestDao,
    private val discoveryRepository: app.lusk.underseerr.domain.repository.DiscoveryRepository,
    private val syncScheduler: SyncScheduler
) : RequestRepository {
    
    private fun scheduleSync() {
        syncScheduler.scheduleOfflineSync()
    }
    
    /**
     * Request a movie.
     * Property 9: Request Submission Completeness
     */
    override suspend fun requestMovie(
        movieId: Int,
        qualityProfile: Int?,
        rootFolder: String?
    ): Result<MediaRequest> {
        val result = safeApiCall {
            requestKtorService.submitRequest(
                app.lusk.underseerr.data.remote.model.ApiRequestBody(
                    mediaId = movieId,
                    mediaType = "movie",
                    profileId = qualityProfile,
                    rootFolder = rootFolder
                )
            )
        }

        return when (result) {
            is Result.Success -> {
                val mediaRequest = result.data.toMediaRequest()
                mediaRequestDao.insert(mediaRequest.toEntity())
                Result.success(mediaRequest)
            }
            is Result.Error -> {
                val error = result.error
                if (error is app.lusk.underseerr.domain.model.AppError.NetworkError || 
                    error is app.lusk.underseerr.domain.model.AppError.TimeoutError) {
                    
                    // Queue for offline
                    val offlineRequest = app.lusk.underseerr.data.local.entity.OfflineRequestEntity(
                        mediaType = "movie",
                        mediaId = movieId,
                        seasons = null,
                        qualityProfile = qualityProfile,
                        rootFolder = rootFolder
                    )
                    offlineRequestDao.insert(offlineRequest)
                    
                    // Also save to MediaRequestDao for UI immediate feedback
                    val dummyRequest = MediaRequest(
                        id = -movieId, // Negative ID to indicate local/temporary
                        mediaType = app.lusk.underseerr.domain.model.MediaType.MOVIE,
                        mediaId = movieId,
                        title = "Queued Request",
                        posterPath = null,
                        status = RequestStatus.PENDING,
                        requestedDate = app.lusk.underseerr.util.nowMillis(),
                        seasons = null,
                        isOfflineQueued = true
                    )
                    mediaRequestDao.insert(dummyRequest.toEntity())
                    
                    scheduleSync()
                    
                    Result.success(dummyRequest)
                } else {
                    Result.error(error)
                }
            }
            else -> result.map { it.toMediaRequest() } // Loading
        }
    }
    
    /**
     * Request a TV show with specific seasons.
     * Property 9: Request Submission Completeness
     */
    override suspend fun requestTvShow(
        tvShowId: Int,
        seasons: List<Int>,
        qualityProfile: Int?,
        rootFolder: String?
    ): Result<MediaRequest> {
        val result = safeApiCall {
            requestKtorService.submitRequest(
                app.lusk.underseerr.data.remote.model.ApiRequestBody(
                    mediaId = tvShowId,
                    mediaType = "tv",
                    seasons = seasons,
                    profileId = qualityProfile,
                    rootFolder = rootFolder
                )
            )
        }

        return when (result) {
            is Result.Success -> {
                val mediaRequest = result.data.toMediaRequest()
                mediaRequestDao.insert(mediaRequest.toEntity())
                Result.success(mediaRequest)
            }
            is Result.Error -> {
                val error = result.error
                if (error is app.lusk.underseerr.domain.model.AppError.NetworkError || 
                    error is app.lusk.underseerr.domain.model.AppError.TimeoutError) {
                    
                    // Queue for offline
                    val offlineRequest = app.lusk.underseerr.data.local.entity.OfflineRequestEntity(
                        mediaType = "tv",
                        mediaId = tvShowId,
                        seasons = seasons.joinToString(","),
                        qualityProfile = qualityProfile,
                        rootFolder = rootFolder
                    )
                    offlineRequestDao.insert(offlineRequest)
                    
                    val dummyRequest = MediaRequest(
                        id = -tvShowId,
                        mediaType = app.lusk.underseerr.domain.model.MediaType.TV,
                        mediaId = tvShowId,
                        title = "Queued TV Request",
                        posterPath = null,
                        status = RequestStatus.PENDING,
                        requestedDate = app.lusk.underseerr.util.nowMillis(),
                        seasons = seasons,
                        isOfflineQueued = true
                    )
                    mediaRequestDao.insert(dummyRequest.toEntity())
                    
                    scheduleSync()
                    
                    Result.success(dummyRequest)
                } else {
                    Result.error(error)
                }
            }
            else -> result.map { it.toMediaRequest() }
        }
    }
    
    /**
     * Get all user requests.
     * Property 13: Request List Completeness
     */
    override fun getUserRequests(): Flow<List<MediaRequest>> {
        return mediaRequestDao.getAllRequests()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Get requests filtered by status.
     * Property 14: Request Grouping Correctness
     */
    override fun getRequestsByStatus(status: RequestStatus): Flow<List<MediaRequest>> {
        val statusCode = when (status) {
            RequestStatus.PENDING -> 1
            RequestStatus.APPROVED -> 2
            RequestStatus.DECLINED -> 3
            RequestStatus.AVAILABLE -> 4
        }
        return mediaRequestDao.getRequestsByStatus(statusCode)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Cancel a pending request.
     * Property 16: Permission-Based Cancellation
     */
    override suspend fun cancelRequest(requestId: Int): Result<Unit> = safeApiCall {
        requestKtorService.deleteRequest(requestId)
        
        // Remove from local cache
        mediaRequestDao.deleteById(requestId)
    }
    
    /**
     * Get request status.
     * Property 17: Request Status Updates
     */
    override suspend fun getRequestStatus(requestId: Int): Result<RequestStatus> = safeApiCall {
        val response = requestKtorService.getRequestStatus(requestId)
        RequestStatus.valueOf(response.status.uppercase())
    }
    
    /**
     * Refresh requests from server.
     * Property 18: Pull-to-Refresh Data Freshness
     */
    override suspend fun refreshRequests(page: Int, pageSize: Int): Result<Unit> = safeApiCall {
        // Calculate offset
        val skip = (page - 1) * pageSize
        
        // Get requests page
        val response = requestKtorService.getRequests(take = pageSize, skip = skip)
        val requests = response.results.map { it.toMediaRequest() }
        
        // Parallel fetch details for requests missing data
        val hydratedRequests = requests.map { request ->
            if (request.title == "Title Unavailable" || request.posterPath == null) {
                // Fetch details
                val hydrated = try {
                    if (request.mediaType == app.lusk.underseerr.domain.model.MediaType.MOVIE) {
                        val movieResult = discoveryRepository.getMovieDetails(request.mediaId)
                        if (movieResult is Result.Success) {
                            request.copy(
                                title = movieResult.data.title,
                                posterPath = movieResult.data.posterPath
                            )
                        } else request
                    } else {
                        val tvResult = discoveryRepository.getTvShowDetails(request.mediaId)
                        if (tvResult is Result.Success) {
                            request.copy(
                                title = tvResult.data.name,
                                posterPath = tvResult.data.posterPath
                            )
                        } else request
                    }
                } catch (e: Exception) {
                    request
                }
                hydrated
            } else {
                request
            }
        }
        
        // Update cache
        try {
            if (page == 1) {
                // If refreshing the first page, we might want to keep existing data until new data arrives
                // but to ensure consistency with a "Pull to Refresh" from top, we often clear or overwrite.
                
                // However, for "requests disappear" issue:
                // Be careful not to delete EVERYTHING if we are only fetching page 1 
                // if we intend to support partial updates.
                // But typically page 1 refresh implies "reset list".
                mediaRequestDao.deleteAll()
            }
            
            mediaRequestDao.insertAll(hydratedRequests.map { it.toEntity() })
        } catch (e: Exception) {
            println("DB Error in refreshRequests: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Check if media is already requested.
     * Property 11: Duplicate Request Prevention
     */
    override suspend fun isMediaRequested(mediaId: Int): Result<Boolean> = safeApiCall {
        val request = mediaRequestDao.getRequestByMediaId(mediaId)
        request != null
    }
    
    /**
     * Get available quality profiles.
     * Property 12: Advanced Options Availability
     */
    override suspend fun getQualityProfiles(isMovie: Boolean): Result<List<QualityProfile>> = safeApiCall {
        val profiles = if (isMovie) {
            val servers = requestKtorService.getRadarrServers()
            val defaultServer = servers.find { it.isDefault } ?: servers.firstOrNull() ?: error("No Radarr server configured")
            requestKtorService.getRadarrService(defaultServer.id).profiles
        } else {
            val servers = requestKtorService.getSonarrServers()
            val defaultServer = servers.find { it.isDefault } ?: servers.firstOrNull() ?: error("No Sonarr server configured")
            requestKtorService.getSonarrService(defaultServer.id).profiles
        }

        profiles.map { QualityProfile(id = it.id, name = it.name) }
    }
    
    /**
     * Get available root folders.
     * Property 12: Advanced Options Availability
     */
    override suspend fun getRootFolders(isMovie: Boolean): Result<List<RootFolder>> = safeApiCall {
        val folders = if (isMovie) {
            val servers = requestKtorService.getRadarrServers()
            val defaultServer = servers.find { it.isDefault } ?: servers.firstOrNull() ?: error("No Radarr server configured")
            requestKtorService.getRadarrService(defaultServer.id).rootFolders
        } else {
            val servers = requestKtorService.getSonarrServers()
            val defaultServer = servers.find { it.isDefault } ?: servers.firstOrNull() ?: error("No Sonarr server configured")
            requestKtorService.getSonarrService(defaultServer.id).rootFolders
        }

        folders.map { RootFolder(id = it.id.toString(), path = it.path) }
    }

    override suspend fun getPartialRequestsEnabled(): Result<Boolean> = safeApiCall {
        requestKtorService.getSystemSettings().partialRequestsEnabled
    }
}
