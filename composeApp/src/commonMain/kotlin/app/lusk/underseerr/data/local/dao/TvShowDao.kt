package app.lusk.underseerr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.lusk.underseerr.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TV Show entities.
 * Feature: underseerr
 * Validates: Requirements 7.1, 7.4
 */
@Dao
interface TvShowDao {
    
    /**
     * Insert a TV show into the database.
     * If the TV show already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tvShow: TvShowEntity)
    
    /**
     * Insert multiple TV shows into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tvShows: List<TvShowEntity>)
    
    /**
     * Update a TV show in the database.
     */
    @Update
    suspend fun update(tvShow: TvShowEntity)
    
    /**
     * Delete a TV show from the database.
     */
    @Delete
    suspend fun delete(tvShow: TvShowEntity)
    
    /**
     * Get a TV show by its ID.
     */
    @Query("SELECT * FROM cached_tv_shows WHERE id = :tvShowId")
    suspend fun getById(tvShowId: Int): TvShowEntity?
    
    /**
     * Get a TV show by its ID as a Flow.
     */
    @Query("SELECT * FROM cached_tv_shows WHERE id = :tvShowId")
    fun getByIdFlow(tvShowId: Int): Flow<TvShowEntity?>
    
    /**
     * Get all cached TV shows.
     */
    @Query("SELECT * FROM cached_tv_shows ORDER BY cachedAt DESC")
    fun getAll(): Flow<List<TvShowEntity>>
    
    /**
     * Get all cached TV shows ordered by cached time.
     */
    @Query("SELECT * FROM cached_tv_shows ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TvShowEntity>
    
    /**
     * Delete all TV shows from the database.
     */
    @Query("DELETE FROM cached_tv_shows")
    suspend fun deleteAll()
    
    /**
     * Delete TV shows older than a specific timestamp.
     */
    @Query("DELETE FROM cached_tv_shows WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    /**
     * Get the count of cached TV shows.
     */
    @Query("SELECT COUNT(*) FROM cached_tv_shows")
    suspend fun getCount(): Int
    
    /**
     * Get the oldest cached TV shows (for LRU eviction).
     */
    @Query("SELECT * FROM cached_tv_shows ORDER BY cachedAt ASC LIMIT :limit")
    suspend fun getOldest(limit: Int): List<TvShowEntity>
}
