package app.lusk.underseerr.domain.repository

import androidx.paging.PagingData
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.SearchResults
import app.lusk.underseerr.domain.model.TvShow
import app.lusk.underseerr.domain.model.Genre
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for media discovery operations.
 * Feature: underseerr
 * Validates: Requirements 2.1, 2.2, 2.4
 */
interface DiscoveryRepository {
    
    /**
     * Get combined trending content (movies and TV shows) with pagination.
     */
    fun getTrending(): Flow<PagingData<app.lusk.underseerr.domain.model.SearchResult>>
    
    /**
     * Search for media (movies and TV shows).
     * Property 5: Search Performance
     * Property 6: Search Result Completeness
     */
    suspend fun searchMedia(query: String, page: Int = 1): Result<SearchResults>
    
    /**
     * Search for media with paging.
     */
    fun findMedia(query: String): Flow<PagingData<app.lusk.underseerr.domain.model.SearchResult>>
    
    /**
     * Get detailed information about a movie.
     * Property 7: Media Detail Navigation
     */
    suspend fun getMovieDetails(movieId: Int): Result<Movie>
    
    /**
     * Get detailed information about a TV show.
     * Property 7: Media Detail Navigation
     */
    suspend fun getTvShowDetails(tvShowId: Int): Result<TvShow>
    
    /**
     * Get popular movies with pagination.
     */
    fun getPopularMovies(): Flow<PagingData<Movie>>
    
    /**
     * Get popular TV shows with pagination.
     */
    fun getPopularTvShows(): Flow<PagingData<TvShow>>

    /**
     * Get upcoming movies with pagination.
     */
    fun getUpcomingMovies(): Flow<PagingData<Movie>>

    /**
     * Get upcoming TV shows with pagination.
     */
    fun getUpcomingTvShows(): Flow<PagingData<TvShow>>

    /**
     * Get user watchlist with paging.
     */
    fun getWatchlist(): Flow<PagingData<app.lusk.underseerr.domain.model.SearchResult>>

    /**
     * Get movie genres.
     */
    suspend fun getMovieGenres(): Result<List<Genre>>

    /**
     * Get TV genres.
     */
    suspend fun getTvGenres(): Result<List<Genre>>

    /**
     * Get movies by genre.
     */
    fun getMoviesByGenre(genreId: Int): Flow<PagingData<Movie>>

    /**
     * Get TV shows by genre.
     */
    fun getTvByGenre(genreId: Int): Flow<PagingData<TvShow>>

    /**
     * Get movies by studio.
     */
    fun getMoviesByStudio(studioId: Int): Flow<PagingData<Movie>>

    /**
     * Get TV shows by network.
     */
    fun getTvByNetwork(networkId: Int): Flow<PagingData<TvShow>>
}
