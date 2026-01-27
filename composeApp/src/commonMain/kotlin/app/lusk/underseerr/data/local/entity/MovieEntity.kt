package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached movie data.
 */
@Entity(tableName = "cached_movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val mediaStatus: Int?,
    val requestId: Int?,
    val available: Boolean,
    val cachedAt: Long
)
