package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovery_cache")
data class DiscoveryCacheEntity(
    @PrimaryKey val sectionKey: String, // e.g., "trending", "popular_movies", "popular_tv"
    val data: String, // JSON serialization of the results
    val cachedAt: Long = app.lusk.underseerr.util.nowMillis()
)
