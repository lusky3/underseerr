package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.model.toMediaRequest
import app.lusk.underseerr.data.remote.api.RequestKtorService
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.data.remote.toAppError
import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.RequestStatus
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.QualityProfile
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.RootFolder
import app.lusk.underseerr.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.*
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
                    
                    // Try to get title/poster from cache since we are offline
                    var cachedTitle = "Queued Request"
                    var cachedPoster: String? = null
                    
                    try {
                        val cachedMovie = discoveryRepository.getMovieDetails(movieId)
                        if (cachedMovie is Result.Success) {
                            cachedTitle = cachedMovie.data.title
                            cachedPoster = cachedMovie.data.posterPath
                        }
                    } catch (e: Exception) {
                        // Ignore, fallback to defaults
                    }

                    // Also save to MediaRequestDao for UI immediate feedback
                    val dummyRequest = MediaRequest(
                        id = -movieId, // Negative ID to indicate local/temporary
                        mediaType = app.lusk.underseerr.domain.model.MediaType.MOVIE,
                        mediaId = movieId,
                        title = cachedTitle,
                        posterPath = cachedPoster,
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
                    
                    // Try to get title/poster from cache since we are offline
                    var cachedTitle = "Queued TV Request"
                    var cachedPoster: String? = null
                    
                    try {
                        val cachedTv = discoveryRepository.getTvShowDetails(tvShowId)
                        if (cachedTv is Result.Success) {
                            cachedTitle = cachedTv.data.name
                            cachedPoster = cachedTv.data.posterPath
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }

                    val dummyRequest = MediaRequest(
                        id = -tvShowId,
                        mediaType = app.lusk.underseerr.domain.model.MediaType.TV,
                        mediaId = tvShowId,
                        title = cachedTitle,
                        posterPath = cachedPoster,
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
    override suspend fun cancelRequest(requestId: Int): Result<Unit> {
        // If requestId is negative, it's an offline request queue item
        if (requestId < 0) {
            val mediaId = -requestId
            offlineRequestDao.deleteByMediaId(mediaId)
            mediaRequestDao.deleteById(requestId)
            return Result.success(Unit)
        }

        return try {
            requestKtorService.deleteRequest(requestId)
            // Remove from local cache on success
            mediaRequestDao.deleteById(requestId)
            Result.success(Unit)
        } catch (e: Exception) {
            val appError = e.toAppError()
            // If 404, valid delete (it's already gone)
            if (appError is app.lusk.underseerr.domain.model.AppError.NotFoundError) {
                mediaRequestDao.deleteById(requestId)
                Result.success(Unit)
            } else {
                Result.error(appError)
            }
        }
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
    override suspend fun refreshRequests(page: Int, pageSize: Int): Result<Pair<Int, Int>> = safeApiCall {
        // Calculate offset
        val skip = (page - 1) * pageSize
        println("RequestRepositoryImpl: Calling getRequests(take=$pageSize, skip=$skip)")
        
        // Get requests page
        val response = requestKtorService.getRequests(take = pageSize, skip = skip)
        val requests = response.results.map { it.toMediaRequest() }
        
        // PARALLEL fetch details for requests missing data
        // Using coroutineScope and async to fetch all details concurrently
        println("RequestRepositoryImpl: Hydrating ${requests.count { it.title == "Title Unavailable" || it.posterPath == null }} items")
        val hydratedRequests = coroutineScope {
            requests.map { request ->
                async {
                    if (request.title == "Title Unavailable" || request.posterPath == null) {
                        try {
                            // Fetch details with a timeout to avoid hanging the entire list
                            withTimeoutOrNull(3000) {
                                if (request.mediaType == app.lusk.underseerr.domain.model.MediaType.MOVIE) {
                                    val movieResult = discoveryRepository.getMovieDetails(request.mediaId)
                                    if (movieResult is Result.Success) {
                                        request.copy(
                                            title = movieResult.data.title,
                                            posterPath = movieResult.data.posterPath
                                        )
                                    } else {
                                        // println("Hydration: Movie details failed for ${request.mediaId}")
                                        request
                                    }
                                } else {
                                    val tvResult = discoveryRepository.getTvShowDetails(request.mediaId)
                                    if (tvResult is Result.Success) {
                                        request.copy(
                                            title = tvResult.data.name,
                                            posterPath = tvResult.data.posterPath
                                        )
                                    } else {
                                        // println("Hydration: TV details failed for ${request.mediaId}")
                                        request
                                    }
                                }
                            } ?: request // Fallback to original if timeout
                        } catch (e: Exception) {
                            println("Hydration: Exception for request ${request.id} (MediaId: ${request.mediaId}): ${e.message}")
                            request
                        }
                    } else {
                        request
                    }
                }
            }.awaitAll()
        }
        
        // Update cache
        try {
            val totalHydrated = hydratedRequests.count { it.title != "Title Unavailable" }
            println("RequestRepositoryImpl: Saving ${hydratedRequests.size} items to DB (Hydrated: $totalHydrated/${hydratedRequests.size}, Total on Server: ${response.pageInfo.results})")
            mediaRequestDao.insertAll(hydratedRequests.map { it.toEntity() })
            val totalInDb = mediaRequestDao.getCount()
            println("RequestRepositoryImpl: Total items in DB after insert: $totalInDb")
        } catch (e: Exception) {
            println("DB Error in refreshRequests: ${e.message}")
            e.printStackTrace()
        }
        
        hydratedRequests.size to response.pageInfo.results
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

    override suspend fun approveRequest(requestId: Int): Result<MediaRequest> {
        val result = safeApiCall {
            requestKtorService.approveRequest(requestId)
        }
        
        return when (result) {
            is Result.Success -> {
                val mediaRequest = result.data.toMediaRequest()
                mediaRequestDao.insert(mediaRequest.toEntity())
                Result.success(mediaRequest)
            }
            is Result.Error -> result
            else -> result.map { it.toMediaRequest() }
        }
    }

    override suspend fun declineRequest(requestId: Int): Result<Unit> {
        return try {
            requestKtorService.declineRequest(requestId)
            // Fetch updated request to update local cache
            try {
                val updatedRequest = requestKtorService.getRequest(requestId)
                mediaRequestDao.insert(updatedRequest.toMediaRequest().toEntity())
            } catch (e: Exception) {
                // Ignore fetch error, but decline was successful
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }

    override suspend fun repairRequest(request: MediaRequest): Result<MediaRequest> {
        return try {
            val updatedRequest = if (request.mediaType == app.lusk.underseerr.domain.model.MediaType.MOVIE) {
                val result = discoveryRepository.getMovieDetails(request.mediaId)
                if (result is Result.Success) {
                    request.copy(
                        title = result.data.title,
                        posterPath = result.data.posterPath
                    )
                } else null
            } else {
                val result = discoveryRepository.getTvShowDetails(request.mediaId)
                if (result is Result.Success) {
                    request.copy(
                        title = result.data.name,
                        posterPath = result.data.posterPath
                    )
                } else null
            }

            if (updatedRequest != null) {
                mediaRequestDao.insert(updatedRequest.toEntity())
                Result.success(updatedRequest)
            } else {
                Result.error(app.lusk.underseerr.domain.model.AppError.NotFoundError("Media details not found"))
            }
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }
}
