package app.lusk.underseerr.domain.model

/**
 * Domain model for an Issue.
 */
data class Issue(
    val id: Int,
    val issueType: IssueType,
    val status: IssueStatus,
    val problemSeason: Int? = null,
    val problemEpisode: Int? = null,
    val mediaTitle: String,
    val mediaPosterPath: String?,
    val mediaType: MediaType,
    val mediaTmdbId: Int?,
    val createdByName: String,
    val createdByAvatar: String?,
    val comments: List<IssueComment>,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Domain model for an Issue Comment.
 */
data class IssueComment(
    val id: Int,
    val userName: String,
    val userAvatar: String?,
    val message: String,
    val createdAt: String?
)

/**
 * Issue types as defined by Overseerr.
 */
enum class IssueType(val value: Int, val displayName: String) {
    VIDEO(1, "Video"),
    AUDIO(2, "Audio"),
    SUBTITLES(3, "Subtitles"),
    OTHER(4, "Other");

    companion object {
        fun fromValue(value: Int): IssueType = entries.find { it.value == value } ?: OTHER
    }
}

/**
 * Issue status.
 */
enum class IssueStatus(val value: Int, val displayName: String) {
    OPEN(1, "Open"),
    RESOLVED(2, "Resolved");

    companion object {
        fun fromValue(value: Int): IssueStatus = entries.find { it.value == value } ?: OPEN
    }
}

/**
 * Issue count statistics.
 */
data class IssueCount(
    val total: Int,
    val video: Int,
    val audio: Int,
    val subtitles: Int,
    val others: Int,
    val open: Int,
    val closed: Int
)
