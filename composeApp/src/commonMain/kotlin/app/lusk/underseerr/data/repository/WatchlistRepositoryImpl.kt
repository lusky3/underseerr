package app.lusk.underseerr.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.paging.WatchlistPagingSource
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.data.remote.api.JellyseerrKtorService
import app.lusk.underseerr.data.remote.api.JellyseerrWatchlistRequest
import app.lusk.underseerr.data.remote.api.PlexKtorService
import app.lusk.underseerr.data.remote.api.PlexWatchlistResponse
import app.lusk.underseerr.data.remote.api.PlexMetadata
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.repository.WatchlistRepository
import app.lusk.underseerr.domain.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WatchlistRepositoryImpl(
    private val discoveryKtorService: DiscoveryKtorService,
    private val plexKtorService: PlexKtorService,
    private val jellyseerrKtorService: JellyseerrKtorService,
    private val authRepository: AuthRepository,
    private val securityManager: SecurityManager,
    private val mediaRequestDao: MediaRequestDao
) : WatchlistRepository {

    override fun getWatchlist(): Flow<PagingData<SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
            pagingSourceFactory = {
                WatchlistPagingSource(
                    plexKtorService = plexKtorService,
                    discoveryKtorService = discoveryKtorService,
                    securityManager = securityManager,
                    mediaRequestDao = mediaRequestDao
                )
            }
        ).flow
    }

    override suspend fun getWatchlistIds(): Result<Set<Int>> {
        return safeApiCall {
            val plexToken = securityManager.retrieveSecureData("plex_token")
            val results = if (plexToken != null) {
                // Fetch first page of Plex watchlist (max 50 for quick check)
                val response = plexKtorService.getWatchlist(plexToken, 1)
                mapPlexWatchlist(response).map { it.id }
            } else {
                // Fallback to Overseerr watchlist
                val response = discoveryKtorService.getWatchlist(1)
                response.results.map { it.id }
            }
            results.toSet()
        }
    }

    private fun mapPlexWatchlist(response: PlexWatchlistResponse): List<SearchResult> {
        return response.mediaContainer.metadata.mapNotNull { metadata ->
            val tmdbId = metadata.tmdbId?.toIntOrNull() 
                ?: metadata.externalGuids
                    .find { it.id.startsWith("tmdb://") }
                    ?.id?.substringAfter("tmdb://")
                    ?.toIntOrNull()

            val finalId = tmdbId ?: metadata.ratingKey.hashCode()
            
            SearchResult(
                id = finalId,
                mediaType = if (metadata.type == "show") app.lusk.underseerr.domain.model.MediaType.TV else app.lusk.underseerr.domain.model.MediaType.MOVIE,
                title = metadata.title,
                overview = metadata.summary ?: "",
                posterPath = metadata.thumb,
                releaseDate = metadata.year?.toString(),
                voteAverage = metadata.rating ?: 0.0,
                ratingKey = metadata.ratingKey
            )
        }
    }

    override suspend fun addToWatchlist(tmdbId: Int, mediaType: String, ratingKey: String?): Result<Unit> {
        return safeApiCall {
            val isJellyseerr = authRepository.getIsJellyseerr().first()
            if (isJellyseerr) {
                jellyseerrKtorService.addToWatchlist(
                    JellyseerrWatchlistRequest(tmdbId = tmdbId, mediaType = mediaType)
                )
            } else {
                val plexToken = securityManager.retrieveSecureData("plex_token")
                    ?: throw Exception("Plex token not found - required for watchlist addition")
                
                val finalRatingKey = ratingKey ?: findPlexRatingKey(plexToken, tmdbId, mediaType)
                
                if (finalRatingKey != null) {
                    plexKtorService.addToWatchlist(plexToken, finalRatingKey)
                } else {
                    throw Exception("Could not find Plex ratingKey for TMDB ID $tmdbId")
                }
            }
        }
    }

    private suspend fun findPlexRatingKey(plexToken: String, tmdbId: Int, mediaType: String): String? {
        // This is a bit of a hack, but we can try to find the ratingKey by searching Plex Discover with tmdb id
        // In many cases, Overseerr already provides it in SearchResult, but for some entries it might be missing.
        // For now, let's assume we need to find it if missing.
        return try {
            val type = if (mediaType == "movie") "movie" else "show"
            // We'd need a Plex search method. Since we don't have one yet, we might just return null 
            // and rely on SearchResult providing it.
            // But let's see if we can implement a basic search in PlexKtorService?
            // Actually, keep it simple for now: if ratingKey is null, it might fail.
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun removeFromWatchlist(tmdbId: Int, ratingKey: String?): Result<Unit> {
        return safeApiCall {
            val isJellyseerr = authRepository.getIsJellyseerr().first()
            if (isJellyseerr) {
                jellyseerrKtorService.removeFromWatchlist(tmdbId)
            } else {
                if (ratingKey == null) throw IllegalArgumentException("RatingKey required for Plex removal")
                val plexToken = securityManager.retrieveSecureData("plex_token") 
                    ?: throw Exception("Plex token not found")
                plexKtorService.removeFromWatchlist(plexToken, ratingKey)
            }
        }
    }
}
