package app.lusk.underseerr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.lusk.underseerr.data.local.entity.MediaRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Media Request entities.
 * Feature: underseerr
 * Validates: Requirements 7.1, 7.4
 */
@Dao
interface MediaRequestDao {
    
    /**
     * Insert a media request into the database.
     * If the request already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: MediaRequestEntity)
    
    /**
     * Insert multiple media requests into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<MediaRequestEntity>)
    
    /**
     * Update a media request in the database.
     */
    @Update
    suspend fun update(request: MediaRequestEntity)
    
    /**
     * Delete a media request from the database.
     */
    @Delete
    suspend fun delete(request: MediaRequestEntity)
    
    /**
     * Get a media request by its ID.
     */
    @Query("SELECT * FROM user_requests WHERE id = :requestId")
    suspend fun getById(requestId: Int): MediaRequestEntity?
    
    /**
     * Get a media request by its ID as a Flow.
     */
    @Query("SELECT * FROM user_requests WHERE id = :requestId")
    fun getByIdFlow(requestId: Int): Flow<MediaRequestEntity?>
    
    /**
     * Get all media requests.
     */
    @Query("SELECT * FROM user_requests ORDER BY requestedDate DESC")
    fun getAll(): Flow<List<MediaRequestEntity>>
    
    /**
     * Get all media requests ordered by requested date.
     */
    @Query("SELECT * FROM user_requests ORDER BY requestedDate DESC")
    suspend fun getAllSync(): List<MediaRequestEntity>
    
    /**
     * Get media requests by status.
     */
    @Query("SELECT * FROM user_requests WHERE status = :status ORDER BY requestedDate DESC")
    fun getByStatus(status: Int): Flow<List<MediaRequestEntity>>
    
    /**
     * Get media requests by media type.
     */
    @Query("SELECT * FROM user_requests WHERE mediaType = :mediaType ORDER BY requestedDate DESC")
    fun getByMediaType(mediaType: String): Flow<List<MediaRequestEntity>>
    
    /**
     * Delete all media requests from the database.
     */
    @Query("DELETE FROM user_requests")
    suspend fun deleteAll()
    
    /**
     * Delete a media request by its ID.
     */
    @Query("DELETE FROM user_requests WHERE id = :requestId")
    suspend fun deleteById(requestId: Int)
    
    /**
     * Get the count of media requests.
     */
    @Query("SELECT COUNT(*) FROM user_requests")
    suspend fun getCount(): Int
    
    /**
     * Get the count of media requests by status.
     */
    @Query("SELECT COUNT(*) FROM user_requests WHERE status = :status")
    suspend fun getCountByStatus(status: Int): Int
    
    /**
     * Get all media requests.
     */
    @Query("SELECT * FROM user_requests ORDER BY requestedDate DESC")
    fun getAllRequests(): Flow<List<MediaRequestEntity>>
    
    /**
     * Get requests by status.
     */
    @Query("SELECT * FROM user_requests WHERE status = :status ORDER BY requestedDate DESC")
    fun getRequestsByStatus(status: Int): Flow<List<MediaRequestEntity>>
    
    /**
     * Get request by media ID.
     */
    @Query("SELECT * FROM user_requests WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getRequestByMediaId(mediaId: Int): MediaRequestEntity?
}
