package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached TV show data.
 */
@Entity(tableName = "cached_tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String?,
    val voteAverage: Double,
    val numberOfSeasons: Int,
    val mediaStatus: Int?,
    val requestId: Int?,
    val available: Boolean,
    val cachedAt: Long
)
