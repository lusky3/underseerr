package app.lusk.underseerr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.lusk.underseerr.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Movie entities.
 * Feature: underseerr
 * Validates: Requirements 7.1, 7.4
 */
@Dao
interface MovieDao {
    
    /**
     * Insert a movie into the database.
     * If the movie already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: MovieEntity)
    
    /**
     * Insert multiple movies into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<MovieEntity>)
    
    /**
     * Update a movie in the database.
     */
    @Update
    suspend fun update(movie: MovieEntity)
    
    /**
     * Delete a movie from the database.
     */
    @Delete
    suspend fun delete(movie: MovieEntity)
    
    /**
     * Get a movie by its ID.
     */
    @Query("SELECT * FROM cached_movies WHERE id = :movieId")
    suspend fun getById(movieId: Int): MovieEntity?
    
    /**
     * Get a movie by its ID as a Flow.
     */
    @Query("SELECT * FROM cached_movies WHERE id = :movieId")
    fun getByIdFlow(movieId: Int): Flow<MovieEntity?>
    
    /**
     * Get all cached movies.
     */
    @Query("SELECT * FROM cached_movies ORDER BY cachedAt DESC")
    fun getAll(): Flow<List<MovieEntity>>
    
    /**
     * Get all cached movies ordered by cached time.
     */
    @Query("SELECT * FROM cached_movies ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MovieEntity>
    
    /**
     * Delete all movies from the database.
     */
    @Query("DELETE FROM cached_movies")
    suspend fun deleteAll()
    
    /**
     * Delete movies older than a specific timestamp.
     */
    @Query("DELETE FROM cached_movies WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    /**
     * Get the count of cached movies.
     */
    @Query("SELECT COUNT(*) FROM cached_movies")
    suspend fun getCount(): Int
    
    /**
     * Get the oldest cached movies (for LRU eviction).
     */
    @Query("SELECT * FROM cached_movies ORDER BY cachedAt ASC LIMIT :limit")
    suspend fun getOldest(limit: Int): List<MovieEntity>
}
