package app.lusk.underseerr.data.paging

import androidx.paging.PagingSource
import app.lusk.underseerr.util.AppConfig
import androidx.paging.PagingState
import app.lusk.underseerr.data.remote.model.ApiSearchResults
import app.lusk.underseerr.data.remote.model.ApiSearchResult

/**
 * Generic PagingSource for discovery feeds.
 */
class DiscoveryPagingSource<T : Any>(
    private val fetcher: suspend (Int) -> ApiSearchResults,
    private val mapper: (ApiSearchResult) -> T,
    private val discoveryDao: app.lusk.underseerr.data.local.dao.DiscoveryDao? = null,
    private val cacheKey: String? = null
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return try {
            debugLog("DiscoveryPagingSource: Loading page $page")
            val response = fetcher(page)
            debugLog("DiscoveryPagingSource: Fetched ${response.results.size} items for page $page")
            val data = response.results.map { mapper(it) }

            // Cache page 1 results
            if (page == 1 && discoveryDao != null && cacheKey != null) {
                try {
                    val json = kotlinx.serialization.json.Json.encodeToString(ApiSearchResults.serializer(), response)
                    discoveryDao.insert(app.lusk.underseerr.data.local.entity.DiscoveryCacheEntity(cacheKey, json))
                } catch (e: Exception) {
                    debugLog("DiscoveryPagingSource: Failed to cache results: ${e.message}")
                }
            }

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            debugLog("DiscoveryPagingSource: Error loading page: ${e.message}")

            // An expired session must surface, not hide behind cached content.
            // Serving the cache here is what made the app look healthy while every
            // request was being rejected. Overseerr uses 403 for "not logged in".
            val status = (e as? io.ktor.client.plugins.ResponseException)?.response?.status?.value
            val isAuthFailure = status == 401 || status == 403

            // Fallback to cache for page 1
            if (!isAuthFailure && page == 1 && discoveryDao != null && cacheKey != null) {
                val cached = discoveryDao.getCache(cacheKey)
                if (cached != null) {
                    try {
                        val response = kotlinx.serialization.json.Json.decodeFromString(ApiSearchResults.serializer(), cached.data)
                        val data = response.results.map { mapper(it) }
                        return LoadResult.Page(
                            data = data,
                            prevKey = null,
                            nextKey = null // Don't allow pagination when in offline cache mode for now
                        )
                    } catch (decodeError: Exception) {
                        debugLog("DiscoveryPagingSource: Failed to decode cache: ${decodeError.message}")
                    }
                }
            }
            
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

/**
 * Release builds must not log request URLs or response bodies. A Ktor
 * ClientRequestException's message contains the full URL plus the response text.
 */
private inline fun debugLog(message: String) {
    if (AppConfig.isDebug) println(message)
}
