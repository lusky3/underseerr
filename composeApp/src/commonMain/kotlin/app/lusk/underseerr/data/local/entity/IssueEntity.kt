package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.lusk.underseerr.domain.model.MediaType

/**
 * Room entity for cached issues.
 */
@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey val id: Int,
    val issueType: Int,
    val status: Int,
    val problemSeason: Int?,
    val problemEpisode: Int?,
    val mediaTitle: String,
    val mediaPosterPath: String?,
    val mediaType: MediaType,
    val mediaTmdbId: Int?,
    val createdByName: String,
    val createdByAvatar: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val cachedAt: Long
)
