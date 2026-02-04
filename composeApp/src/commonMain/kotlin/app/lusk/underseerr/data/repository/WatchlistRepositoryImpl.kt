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

    override suspend fun addToWatchlist(tmdbId: Int, mediaType: String, ratingKey: String?): Result<Unit> {
        return safeApiCall {
            val isJellyseerr = authRepository.getIsJellyseerr().first()
            if (isJellyseerr) {
                jellyseerrKtorService.addToWatchlist(
                    JellyseerrWatchlistRequest(tmdbId = tmdbId, mediaType = mediaType)
                )
            } else {
                // Overseerr usually uses Plex for watchlist addition, but we don't have a direct 'add to watchlist' 
                // capability in PlexKtorService currently exposed or verified?
                // Also Overseerr UI usually triggers this via Plex directly or via Overseerr proxy request?
                // For now, if not Jellyseerr, we might not support ADDING via this app if it wasn't there before.
                // But wait, the user wants "Abstract watchlist...".
                // If it's supported for Plex, implemented it. If not, maybe implement later or assume it's not supported yet for Plex.
                // Currently only 'removeFromWatchlist' was in DiscoveryRepo.
                throw NotImplementedError("Adding to watchlist for Plex/Overseerr not yet implemented")
            }
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
