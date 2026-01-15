package app.lusk.client.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.lusk.client.data.remote.api.DiscoveryKtorService
import app.lusk.client.data.remote.model.toSearchResult
import app.lusk.client.domain.model.SearchResult

/**
 * PagingSource for search results.
 */
class SearchPagingSource(
    private val discoveryKtorService: DiscoveryKtorService,
    private val query: String
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val page = params.key ?: 1
        return try {
            val response = discoveryKtorService.search(query, page)
            val results = response.results.map { it.toSearchResult() }
            
            LoadResult.Page(
                data = results,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (results.isEmpty() || page >= response.totalPages) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
