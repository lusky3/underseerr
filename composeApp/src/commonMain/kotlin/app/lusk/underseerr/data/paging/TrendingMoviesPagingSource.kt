package app.lusk.underseerr.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.lusk.underseerr.data.remote.model.toMovie
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.domain.model.Movie

/**
 * PagingSource for trending movies.
 * Feature: underseerr
 * Validates: Requirements 2.1, 2.5
 */
class TrendingMoviesPagingSource(
    private val discoveryKtorService: DiscoveryKtorService
) : PagingSource<Int, Movie>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        return try {
            val page = params.key ?: 1
            val response = discoveryKtorService.getTrendingMovies(page)
            
            val movies = response.results.map { it.toMovie() }
            
            LoadResult.Page(
                data = movies,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < response.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
