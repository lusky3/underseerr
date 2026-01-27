package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MovieDao
import app.lusk.underseerr.data.local.dao.TvShowDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.TvShow
import app.lusk.underseerr.domain.repository.CacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock as KClock

/**
 * Implementation of CacheRepository for managing cached media data.
 * Feature: underseerr, Property 29: Cache Eviction Policy
 * Validates: Requirements 7.1, 7.4
 */
class CacheRepositoryImpl(
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao
) : CacheRepository {
    
    companion object {
        // Maximum cache size in bytes (100 MB)
        private const val MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024L
        
        // Estimated average size per movie entry (in bytes)
        private const val ESTIMATED_MOVIE_SIZE = 5 * 1024 // 5 KB
        
        // Estimated average size per TV show entry (in bytes)
        private const val ESTIMATED_TV_SHOW_SIZE = 5 * 1024 // 5 KB
        
        // Maximum age for cached items (7 days)
        private const val MAX_CACHE_AGE_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
    
    override suspend fun cacheMovie(movie: Movie) {
        val entity = movie.toEntity()
        movieDao.insert(entity)
        
        // Check if we need to evict old items
        evictLeastRecentlyUsed()
    }
    
    override suspend fun cacheMovies(movies: List<Movie>) {
        val entities = movies.map { it.toEntity() }
        movieDao.insertAll(entities)
        
        // Check if we need to evict old items
        evictLeastRecentlyUsed()
    }
    
    override suspend fun getCachedMovie(movieId: Int): Movie? {
        return movieDao.getById(movieId)?.toDomain()
    }
    
    override fun getCachedMovieFlow(movieId: Int): Flow<Movie?> {
        return movieDao.getByIdFlow(movieId).map { it?.toDomain() }
    }
    
    override suspend fun cacheTvShow(tvShow: TvShow) {
        val entity = tvShow.toEntity()
        tvShowDao.insert(entity)
        
        // Check if we need to evict old items
        evictLeastRecentlyUsed()
    }
    
    override suspend fun cacheTvShows(tvShows: List<TvShow>) {
        val entities = tvShows.map { it.toEntity() }
        tvShowDao.insertAll(entities)
        
        // Check if we need to evict old items
        evictLeastRecentlyUsed()
    }
    
    override suspend fun getCachedTvShow(tvShowId: Int): TvShow? {
        return tvShowDao.getById(tvShowId)?.toDomain()
    }
    
    override fun getCachedTvShowFlow(tvShowId: Int): Flow<TvShow?> {
        return tvShowDao.getByIdFlow(tvShowId).map { it?.toDomain() }
    }
    
    override fun getAllCachedMovies(): Flow<List<Movie>> {
        return movieDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllCachedTvShows(): Flow<List<TvShow>> {
        return tvShowDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun clearMovieCache() {
        movieDao.deleteAll()
    }
    
    override suspend fun clearTvShowCache() {
        tvShowDao.deleteAll()
    }
    
    override suspend fun clearAllCaches() {
        movieDao.deleteAll()
        tvShowDao.deleteAll()
    }
    
    override suspend fun getCacheSize(): Long {
        val movieCount = movieDao.getCount()
        val tvShowCount = tvShowDao.getCount()
        
        // Estimate cache size based on entry counts
        return (movieCount * ESTIMATED_MOVIE_SIZE + tvShowCount * ESTIMATED_TV_SHOW_SIZE).toLong()
    }
    
    override suspend fun evictLeastRecentlyUsed() {
        val currentSize = getCacheSize()
        
        if (currentSize > MAX_CACHE_SIZE_BYTES) {
            // Calculate how many items to remove (remove 20% of items)
            val movieCount = movieDao.getCount()
            val tvShowCount = tvShowDao.getCount()
            
            val moviesToRemove = (movieCount * 0.2).toInt()
            val tvShowsToRemove = (tvShowCount * 0.2).toInt()
            
            // Get oldest items and delete them
            if (moviesToRemove > 0) {
                val oldestMovies = movieDao.getOldest(moviesToRemove)
                oldestMovies.forEach { movieDao.delete(it) }
            }
            
            if (tvShowsToRemove > 0) {
                val oldestTvShows = tvShowDao.getOldest(tvShowsToRemove)
                oldestTvShows.forEach { tvShowDao.delete(it) }
            }
        }
    }
    
    override suspend fun deleteOlderThan(ageMillis: Long) {
        val cutoffTimestamp = app.lusk.underseerr.util.nowMillis() - ageMillis
        movieDao.deleteOlderThan(cutoffTimestamp)
        tvShowDao.deleteOlderThan(cutoffTimestamp)
    }
}
