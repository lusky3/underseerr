package app.lusk.underseerr.data.local.dao

import androidx.room.*
import app.lusk.underseerr.data.local.entity.DiscoveryCacheEntity

@Dao
interface DiscoveryDao {
    @Query("SELECT * FROM discovery_cache WHERE sectionKey = :key")
    suspend fun getCache(key: String): DiscoveryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: DiscoveryCacheEntity)

    @Query("DELETE FROM discovery_cache WHERE sectionKey = :key")
    suspend fun clear(key: String)
}
