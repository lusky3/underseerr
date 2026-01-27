package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.SearchResults
import app.lusk.underseerr.domain.model.TvShow
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for caching media data.
 * Feature: underseerr, Property 29: Cache Eviction Policy
 * Validates: Requirements 7.1, 7.4
 */
interface CacheRepository {
    
    /**
     * Cache a movie.
     */
    suspend fun cacheMovie(movie: Movie)
    
    /**
     * Cache multiple movies.
     */
    suspend fun cacheMovies(movies: List<Movie>)
    
    /**
     * Get a cached movie by ID.
     */
    suspend fun getCachedMovie(movieId: Int): Movie?
    
    /**
     * Get a cached movie by ID as a Flow.
     */
    fun getCachedMovieFlow(movieId: Int): Flow<Movie?>
    
    /**
     * Cache a TV show.
     */
    suspend fun cacheTvShow(tvShow: TvShow)
    
    /**
     * Cache multiple TV shows.
     */
    suspend fun cacheTvShows(tvShows: List<TvShow>)
    
    /**
     * Get a cached TV show by ID.
     */
    suspend fun getCachedTvShow(tvShowId: Int): TvShow?
    
    /**
     * Get a cached TV show by ID as a Flow.
     */
    fun getCachedTvShowFlow(tvShowId: Int): Flow<TvShow?>
    
    /**
     * Get all cached movies.
     */
    fun getAllCachedMovies(): Flow<List<Movie>>
    
    /**
     * Get all cached TV shows.
     */
    fun getAllCachedTvShows(): Flow<List<TvShow>>
    
    /**
     * Clear all cached movies.
     */
    suspend fun clearMovieCache()
    
    /**
     * Clear all cached TV shows.
     */
    suspend fun clearTvShowCache()
    
    /**
     * Clear all caches.
     */
    suspend fun clearAllCaches()
    
    /**
     * Get the total cache size in bytes.
     */
    suspend fun getCacheSize(): Long
    
    /**
     * Evict least recently used items if cache exceeds size limit.
     */
    suspend fun evictLeastRecentlyUsed()
    
    /**
     * Delete items older than a specific age.
     */
    suspend fun deleteOlderThan(ageMillis: Long)
}
