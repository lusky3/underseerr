package app.lusk.underseerr.data.paging

import androidx.paging.PagingSource
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
            println("DiscoveryPagingSource: Loading page $page")
            val response = fetcher(page)
            println("DiscoveryPagingSource: Fetched ${response.results.size} items for page $page")
            val data = response.results.map { mapper(it) }

            // Cache page 1 results
            if (page == 1 && discoveryDao != null && cacheKey != null) {
                try {
                    val json = kotlinx.serialization.json.Json.encodeToString(ApiSearchResults.serializer(), response)
                    discoveryDao.insert(app.lusk.underseerr.data.local.entity.DiscoveryCacheEntity(cacheKey, json))
                } catch (e: Exception) {
                    println("DiscoveryPagingSource: Failed to cache results: ${e.message}")
                }
            }

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            println("DiscoveryPagingSource: Error loading page: ${e.message}")
            
            // Fallback to cache for page 1
            if (page == 1 && discoveryDao != null && cacheKey != null) {
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
                        println("DiscoveryPagingSource: Failed to decode cache: ${decodeError.message}")
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
