package app.lusk.underseerr.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.lusk.underseerr.data.remote.model.toTvShow
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.domain.model.TvShow

/**
 * PagingSource for trending TV shows.
 * Feature: underseerr
 * Validates: Requirements 2.1, 2.5
 */
class TrendingTvShowsPagingSource(
    private val discoveryKtorService: DiscoveryKtorService
) : PagingSource<Int, TvShow>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TvShow> {
        return try {
            val page = params.key ?: 1
            val response = discoveryKtorService.getTrendingTvShows(page)
            
            val tvShows = response.results.map { it.toTvShow() }
            
            LoadResult.Page(
                data = tvShows,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, TvShow>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
