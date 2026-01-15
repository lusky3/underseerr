package app.lusk.client.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.lusk.client.data.remote.model.ApiSearchResults
import app.lusk.client.data.remote.model.ApiSearchResult

/**
 * Generic PagingSource for discovery feeds.
 */
class DiscoveryPagingSource<T : Any>(
    private val fetcher: suspend (Int) -> ApiSearchResults,
    private val mapper: (ApiSearchResult) -> T
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val page = params.key ?: 1
            val response = fetcher(page)
            val data = response.results.map { mapper(it) }

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
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
