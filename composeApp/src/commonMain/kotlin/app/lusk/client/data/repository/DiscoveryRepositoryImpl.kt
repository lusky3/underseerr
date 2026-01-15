package app.lusk.client.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.lusk.client.data.remote.model.toMovie
import app.lusk.client.data.remote.model.toSearchResults
import app.lusk.client.data.remote.model.toTvShow
import app.lusk.client.data.remote.model.toSearchResult
import app.lusk.client.data.mapper.toEntity
import app.lusk.client.data.mapper.toDomain
import app.lusk.client.data.paging.SearchPagingSource
import app.lusk.client.data.paging.TrendingMoviesPagingSource
import app.lusk.client.data.paging.TrendingTvShowsPagingSource
import app.lusk.client.data.paging.DiscoveryPagingSource
import app.lusk.client.data.remote.api.DiscoveryKtorService
import app.lusk.client.data.remote.model.toGenre
import app.lusk.client.data.remote.safeApiCall
import app.lusk.client.domain.model.Movie
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.SearchResults
import app.lusk.client.domain.model.TvShow
import app.lusk.client.domain.model.Genre
import app.lusk.client.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of DiscoveryRepository for media discovery operations.
 * Feature: overseerr-android-client
 * Validates: Requirements 2.1, 2.2, 2.4
 */
class DiscoveryRepositoryImpl(
    private val discoveryKtorService: DiscoveryKtorService,
    private val movieDao: app.lusk.client.data.local.dao.MovieDao,
    private val tvShowDao: app.lusk.client.data.local.dao.TvShowDao,
    private val mediaRequestDao: app.lusk.client.data.local.dao.MediaRequestDao
) : DiscoveryRepository {
    
    companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
    }
    
    override fun getTrending(): Flow<PagingData<app.lusk.client.domain.model.SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getTrending(it) }, { it.toSearchResult() }) }
        ).flow
    }
    
    override suspend fun searchMedia(query: String, page: Int): Result<SearchResults> {
        return safeApiCall {
            val apiSearchResults = discoveryKtorService.search(query, page)
            apiSearchResults.toSearchResults()
        }
    }
    
    override fun findMedia(query: String): Flow<PagingData<app.lusk.client.domain.model.SearchResult>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SearchPagingSource(discoveryKtorService, query)
            }
        ).flow
    }
    
    override suspend fun getMovieDetails(movieId: Int): Result<Movie> {
        // 1. Try API
        val result = safeApiCall { discoveryKtorService.getMovieDetails(movieId) }
        
        if (result is Result.Success) {
            val movie = result.data.toMovie()
            
            // Check local request status to override if needed (e.g. offline queued)
            val localRequest = mediaRequestDao.getRequestByMediaId(movieId)
            val finalMovie = if (localRequest != null && movie.mediaInfo?.status != app.lusk.client.domain.model.MediaStatus.AVAILABLE) {
                // If we have a local request AND media is not yet available, use request-derived status
                val newStatus = when (localRequest.status) {
                    1 -> app.lusk.client.domain.model.MediaStatus.PENDING    // PENDING
                    2 -> app.lusk.client.domain.model.MediaStatus.PROCESSING // APPROVED
                    4, 5 -> app.lusk.client.domain.model.MediaStatus.AVAILABLE // AVAILABLE
                    else -> app.lusk.client.domain.model.MediaStatus.UNKNOWN
                }
                
                val currentInfo = movie.mediaInfo ?: app.lusk.client.domain.model.MediaInfo(
                    id = null, status = newStatus, requestId = localRequest.id, available = false
                )
                movie.copy(mediaInfo = currentInfo.copy(status = newStatus, requestId = localRequest.id))
            } else {
                movie
            }

            // Save to DB
            movieDao.insert(finalMovie.toEntity())
            
            return Result.success(finalMovie)
        } 
        
        // 2. Fallback to Cache
        if (result is Result.Error && (result.error is app.lusk.client.domain.model.AppError.NetworkError || 
            result.error is app.lusk.client.domain.model.AppError.TimeoutError)) {
            
            val cached = movieDao.getById(movieId)
            if (cached != null) {
                val movie = cached.toDomain()
                return Result.success(movie)
            }
        }
        
        // Return error if we have one
        if (result is Result.Error) return Result.error(result.error)
        
        return Result.loading()
    }
    
    override suspend fun getTvShowDetails(tvShowId: Int): Result<TvShow> {
        // 1. Try API
        val result = safeApiCall { discoveryKtorService.getTvShowDetails(tvShowId) }
        
        if (result is Result.Success) {
            val tvShow = result.data.toTvShow()
            
            val localRequest = mediaRequestDao.getRequestByMediaId(tvShowId)
            val finalTvShow = if (localRequest != null && tvShow.mediaInfo?.status != app.lusk.client.domain.model.MediaStatus.AVAILABLE) {
                val newStatus = when (localRequest.status) {
                    1 -> app.lusk.client.domain.model.MediaStatus.PENDING    // PENDING
                    2 -> app.lusk.client.domain.model.MediaStatus.PROCESSING // APPROVED
                    4, 5 -> app.lusk.client.domain.model.MediaStatus.AVAILABLE // AVAILABLE
                    else -> app.lusk.client.domain.model.MediaStatus.UNKNOWN
                }
                
                val currentInfo = tvShow.mediaInfo ?: app.lusk.client.domain.model.MediaInfo(
                    id = null, status = newStatus, requestId = localRequest.id, available = false
                )
                tvShow.copy(mediaInfo = currentInfo.copy(status = newStatus, requestId = localRequest.id))
            } else {
                tvShow
            }
            
            tvShowDao.insert(finalTvShow.toEntity())
            return Result.success(finalTvShow)
        }
        
        if (result is Result.Error && (result.error is app.lusk.client.domain.model.AppError.NetworkError || 
            result.error is app.lusk.client.domain.model.AppError.TimeoutError)) {
            
            val cached = tvShowDao.getById(tvShowId)
            if (cached != null) {
                val tvShow = cached.toDomain()
                return Result.success(tvShow)
            }
        }
        
        if (result is Result.Error) return Result.error(result.error)
        
        return Result.loading()
    }
    
    override fun getPopularMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getPopularMovies(it) }, { it.toMovie() }) }
        ).flow
    }
    
    override fun getPopularTvShows(): Flow<PagingData<TvShow>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getPopularTvShows(it) }, { it.toTvShow() }) }
        ).flow
    }

    override fun getUpcomingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getUpcomingMovies(it) }, { it.toMovie() }) }
        ).flow
    }

    override fun getUpcomingTvShows(): Flow<PagingData<TvShow>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getUpcomingTvShows(it) }, { it.toTvShow() }) }
        ).flow
    }

    override fun getWatchlist(): Flow<PagingData<app.lusk.client.domain.model.SearchResult>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getWatchlist(it) }, { it.toSearchResult() }) }
        ).flow
    }

    override suspend fun getMovieGenres(): Result<List<Genre>> = safeApiCall {
        discoveryKtorService.getMovieGenres().map { it.toGenre() }
    }

    override suspend fun getTvGenres(): Result<List<Genre>> = safeApiCall {
        discoveryKtorService.getTvGenres().map { it.toGenre() }
    }

    override fun getMoviesByGenre(genreId: Int): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getMoviesByGenre(genreId, it) }, { it.toMovie() }) }
        ).flow
    }

    override fun getTvByGenre(genreId: Int): Flow<PagingData<TvShow>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getTvByGenre(genreId, it) }, { it.toTvShow() }) }
        ).flow
    }

    override fun getMoviesByStudio(studioId: Int): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getStudioDetails(studioId, it) }, { it.toMovie() }) }
        ).flow
    }

    override fun getTvByNetwork(networkId: Int): Flow<PagingData<TvShow>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE, enablePlaceholders = false),
            pagingSourceFactory = { DiscoveryPagingSource({ discoveryKtorService.getNetworkDetails(networkId, it) }, { it.toTvShow() }) }
        ).flow
    }
}
