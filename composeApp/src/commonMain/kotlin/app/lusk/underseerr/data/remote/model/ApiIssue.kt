package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API response for a list of issues with pagination.
 */
@Serializable
data class ApiIssueListResponse(
    val pageInfo: PageInfo? = null,
    val results: List<ApiIssue> = emptyList()
)

/**
 * API model for an Issue.
 */
@Serializable
data class ApiIssue(
    val id: Int,
    val issueType: Int,
    val status: Int = 1, // 1 = open, 2 = resolved
    val problemSeason: Int? = null,
    val problemEpisode: Int? = null,
    val media: ApiIssueMedia? = null,
    val createdBy: ApiUserProfile? = null,
    val modifiedBy: ApiUserProfile? = null,
    val comments: List<ApiIssueComment> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * API model for media in an Issue (contains title and other info).
 */
@Serializable
data class ApiIssueMedia(
    @SerialName("id") val id: Int? = null,
    @SerialName("tmdbId") val tmdbId: Int? = null,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("mediaType") val mediaType: String? = null,
    @SerialName("status") val status: Int = 1,
    // Movie fields
    @SerialName("title") val title: String? = null,
    @SerialName("originalTitle") val originalTitle: String? = null,
    // TV fields
    @SerialName("name") val name: String? = null,
    @SerialName("originalName") val originalName: String? = null,
    // Common fields
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null
)

/**
 * API model for an Issue Comment.
 */
@Serializable
data class ApiIssueComment(
    val id: Int,
    val user: ApiUserProfile? = null,
    val message: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * API response for issue counts.
 */
@Serializable
data class ApiIssueCount(
    val total: Int = 0,
    val video: Int = 0,
    val audio: Int = 0,
    val subtitles: Int = 0,
    val others: Int = 0,
    val open: Int = 0,
    val closed: Int = 0
)

/**
 * API request body for creating a new issue.
 */
@Serializable
data class ApiCreateIssueRequest(
    val issueType: Int,
    val message: String,
    val mediaId: Int,
    val problemSeason: Int? = 0,
    val problemEpisode: Int? = 0
)
