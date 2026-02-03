package app.lusk.underseerr.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.data.remote.api.PlexKtorService
import app.lusk.underseerr.data.remote.api.PlexWatchlistResponse
import app.lusk.underseerr.data.remote.model.ApiSearchResults
import app.lusk.underseerr.data.remote.model.ApiSearchResult
import app.lusk.underseerr.data.remote.model.toSearchResult
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.domain.security.SecurityManager

class WatchlistPagingSource(
    private val plexKtorService: PlexKtorService,
    private val discoveryKtorService: DiscoveryKtorService,
    private val securityManager: SecurityManager
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val page = params.key ?: 1
        return try {
            val plexToken = securityManager.retrieveSecureData("plex_token")
            
            val response: ApiSearchResults = if (plexToken != null) {
                println("WatchlistPagingSource: Fetching from Plex (token present)")
                plexKtorService.getWatchlist(plexToken, page).toApiSearchResults()
            } else {
                println("WatchlistPagingSource: Falling back to Overseerr (no Plex token)")
                discoveryKtorService.getWatchlist(page)
            }

            val data = response.results.map { it.toSearchResult() }

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            println("WatchlistPagingSource: Error loading page: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    private fun PlexWatchlistResponse.toApiSearchResults(): ApiSearchResults {
        val mappedResults = mediaContainer.metadata.mapNotNull { metadata ->
            val tmdbId = metadata.tmdbId?.toIntOrNull() 
                ?: metadata.externalGuids
                    .find { it.id.startsWith("tmdb://") }
                    ?.id?.substringAfter("tmdb://")
                    ?.toIntOrNull()

            // If we still don't have a TMDB ID, we can't reliably show it in Overseerr UI
            // but for now let's use ratingKey as fallback so it appears
            val finalId = tmdbId ?: metadata.ratingKey.hashCode() // Use hash as int fallback

            ApiSearchResult(
                id = finalId,
                mediaType = if (metadata.type == "show") "tv" else "movie",
                title = metadata.title,
                overview = metadata.summary,
                posterPath = metadata.thumb,
                releaseDate = metadata.year?.toString(),
                voteAverage = metadata.rating ?: 0.0
            )
        }

        return ApiSearchResults(
            page = (mediaContainer.offset ?: 0) / 20 + 1,
            totalPages = ((mediaContainer.totalSize ?: mediaContainer.size) + 19) / 20,
            totalResults = mediaContainer.totalSize ?: mediaContainer.size,
            results = mappedResults
        )
    }
}
