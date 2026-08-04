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
import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.repository.WatchlistRepository
import app.lusk.underseerr.domain.security.SecurityManager
import io.ktor.client.plugins.ResponseException
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
            val plexToken = securityManager.retrieveSecureData(PLEX_TOKEN_KEY)
            val results = if (!plexToken.isNullOrEmpty()) {
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
                mediaType = if (metadata.type == "show") MediaType.TV else MediaType.MOVIE,
                title = metadata.title,
                overview = metadata.summary ?: "",
                posterPath = metadata.thumb,
                releaseDate = metadata.year?.toString(),
                voteAverage = metadata.rating ?: 0.0,
                ratingKey = metadata.ratingKey
            )
        }
    }

    /**
     * Runs a watchlist mutation against whichever backend owns the watchlist.
     *
     * Jellyseerr has its own watchlist endpoints. Overseerr does not — its
     * `/discover/watchlist` is a read-only mirror of Plex Discover — so an
     * Overseerr install can only be written to via Plex. When the Plex token has
     * been dropped (revoked upstream, see HttpClientFactory) reads still fall
     * back to Overseerr, but writes are impossible until the user re-links Plex.
     * Report that as a typed, actionable error rather than a raw exception.
     *
     * The *first* write after a revocation does not see an absent token: the token
     * is still in storage, plex.tv answers 401, and only then does the HTTP client
     * delete it. Without [plexCall] that first attempt would surface a raw HTTP
     * error and only a second attempt would get the friendly message, so a 401 from
     * plex.tv is mapped to the same [AppError.PlexReauthRequired].
     */
    private suspend fun runWatchlistWrite(
        onJellyseerr: suspend () -> Unit,
        onPlex: suspend (plexToken: String) -> Unit
    ): Result<Unit> {
        val outcome = safeApiCall {
            if (authRepository.getIsJellyseerr().first()) {
                onJellyseerr()
                true
            } else {
                val plexToken = securityManager.retrieveSecureData(PLEX_TOKEN_KEY)
                // isNullOrEmpty, not == null: an empty string is not a usable token,
                // and SessionRefresher already treats the two the same for this key.
                if (plexToken.isNullOrEmpty()) {
                    false
                } else {
                    plexCall { onPlex(plexToken) }
                    true
                }
            }
        }

        return when (outcome) {
            is Result.Success ->
                if (outcome.data) Result.success(Unit)
                else Result.error(AppError.PlexReauthRequired())
            is Result.Error -> {
                val rejection = outcome.error.cause as? PlexTokenRejected
                if (rejection != null) Result.error(AppError.PlexReauthRequired(cause = rejection.cause))
                else Result.error(outcome.error)
            }
            is Result.Loading -> Result.loading()
        }
    }

    /**
     * Runs a plex.tv call, tagging a 401 so [runWatchlistWrite] can report it as
     * "re-link Plex" instead of letting `safeApiCall` turn it into a bare
     * `AppError.AuthError` ("Authentication required") that points the user at the
     * wrong account.
     */
    private suspend fun <T> plexCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: ResponseException) {
            if (e.response.status.value == 401) throw PlexTokenRejected(e) else throw e
        }

    /** Internal marker; never surfaced to the UI. See [plexCall]. */
    private class PlexTokenRejected(cause: Throwable) :
        Exception("Plex rejected the stored token", cause)

    override suspend fun addToWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String?): Result<Unit> {
        return runWatchlistWrite(
            onJellyseerr = {
                val mediaTypeString = if (mediaType == MediaType.TV) "tv" else "movie"
                jellyseerrKtorService.addToWatchlist(
                    JellyseerrWatchlistRequest(tmdbId = tmdbId, mediaType = mediaTypeString)
                )
            },
            onPlex = { plexToken ->
                val plexMediaType = if (mediaType == MediaType.TV) "show" else "movie"
                val finalRatingKey = ratingKey ?: findPlexRatingKey(plexToken, tmdbId, plexMediaType)

                if (finalRatingKey != null) {
                    plexKtorService.addToWatchlist(plexToken, finalRatingKey)
                } else {
                    throw Exception("Could not find Plex ratingKey for TMDB ID $tmdbId")
                }
            }
        )
    }

    private suspend fun findPlexRatingKey(plexToken: String, tmdbId: Int, plexMediaType: String): String? {
        return try {
            println("WatchlistRepository: Searching Plex for TMDB ID $tmdbId ($plexMediaType)")
            
            // 1. Fetch details to get Title and Year
            val title: String
            val year: Int?
            
            if (plexMediaType == "movie") {
                val details = discoveryKtorService.getMovieDetails(tmdbId, "en")
                title = details.title
                year = details.releaseDate?.take(4)?.toIntOrNull()
            } else {
                val details = discoveryKtorService.getTvShowDetails(tmdbId, "en")
                title = details.name
                year = details.firstAirDate?.take(4)?.toIntOrNull()
            }
            
            println("WatchlistRepository: Searching Plex for '$title' ($year)")
            // Only the Plex leg is tagged: a 401 from the Overseerr detail lookups
            // above is an Overseerr problem and must not be reported as "re-link Plex".
            val response = plexCall { plexKtorService.searchDiscover(plexToken, title, plexMediaType) }
            
            // 2. Iterate and match
            val allResults = response.mediaContainer.searchResults
                .flatMap { it.searchResult }
                .map { it.metadata } + response.mediaContainer.metadata

            val match = allResults.firstOrNull { metadata ->
                val titleMatch = metadata.title.equals(title, ignoreCase = true)
                val yearMatch = if (year != null && metadata.year != null) metadata.year == year else true
                titleMatch && yearMatch
            }

            val ratingKey = match?.ratingKey
            
            if (ratingKey != null) {
                println("WatchlistRepository: Found Plex ratingKey $ratingKey for '$title'")
            } else {
                println("WatchlistRepository: No match found for '$title' ($year) in Plex results")
            }
            
            ratingKey
        } catch (e: PlexTokenRejected) {
            // A revoked token must not be flattened into "no match found" — that would
            // turn the actionable re-link prompt into "Could not find Plex ratingKey".
            throw e
        } catch (e: Exception) {
            println("WatchlistRepository: Failed to find Plex ratingKey for TMDB ID $tmdbId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun removeFromWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String?): Result<Unit> {
        return runWatchlistWrite(
            onJellyseerr = {
                jellyseerrKtorService.removeFromWatchlist(tmdbId)
            },
            onPlex = { plexToken ->
                val plexMediaType = if (mediaType == MediaType.TV) "show" else "movie"
                val finalRatingKey = ratingKey ?: findPlexRatingKey(plexToken, tmdbId, plexMediaType)

                if (finalRatingKey != null) {
                    println("WatchlistRepository: Removing from Plex watchlist with ratingKey $finalRatingKey")
                    try {
                        plexKtorService.removeFromWatchlist(plexToken, finalRatingKey)
                    } catch (e: io.ktor.client.plugins.ClientRequestException) {
                        if (e.response.status.value == 404) {
                            println("WatchlistRepository: Item already removed (404), treating as success")
                        } else {
                            throw e
                        }
                    }
                } else {
                    throw Exception("Could not find Plex ratingKey for TMDB ID $tmdbId")
                }
            }
        )
    }

    private companion object {
        const val PLEX_TOKEN_KEY = "plex_token"
    }
}
