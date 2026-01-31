package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.remote.model.ApiIssue
import app.lusk.underseerr.data.remote.model.ApiIssueComment
import app.lusk.underseerr.data.remote.model.ApiIssueCount
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueComment
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.IssueStatus
import app.lusk.underseerr.domain.model.IssueType
import app.lusk.underseerr.domain.model.MediaType

/**
 * Map API Issue to domain Issue.
 */
fun ApiIssue.toDomain(): Issue {
    val mediaType = when (media?.mediaType) {
        "tv" -> MediaType.TV
        else -> MediaType.MOVIE
    }
    
    // Get title from media - movies use 'title', TV shows use 'name'
    // The API may not always return title in the issue response, so we build a fallback
    val mediaTitle = media?.let {
        // Try title first (movie), then name (TV), then originalTitle, then originalName
        val apiTitle = it.title ?: it.name ?: it.originalTitle ?: it.originalName
        if (apiTitle != null) {
            apiTitle
        } else {
            // Fallback: use TMDB ID with media type label
            val typeLabel = if (mediaType == MediaType.TV) "TV Show" else "Movie"
            "$typeLabel (TMDB: ${it.tmdbId ?: "Unknown"})"
        }
    } ?: "Unknown Media"
    
    // Only show season/episode for TV shows, and only if they have meaningful values (> 0)
    val showSeason = mediaType == MediaType.TV && problemSeason != null && problemSeason > 0
    val showEpisode = mediaType == MediaType.TV && problemEpisode != null && problemEpisode > 0
    
    return Issue(
        id = id,
        issueType = IssueType.fromValue(issueType),
        status = IssueStatus.fromValue(status),
        problemSeason = if (showSeason) problemSeason else null,
        problemEpisode = if (showEpisode) problemEpisode else null,
        mediaTitle = mediaTitle,
        mediaPosterPath = media?.posterPath,
        mediaType = mediaType,
        mediaTmdbId = media?.tmdbId,
        createdByName = createdBy?.displayName ?: createdBy?.email ?: "Unknown",
        createdByAvatar = createdBy?.avatar,
        comments = comments.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Map API IssueComment to domain IssueComment.
 */
fun ApiIssueComment.toDomain(): IssueComment {
    return IssueComment(
        id = id,
        userName = user?.displayName ?: user?.email ?: "Unknown",
        userAvatar = user?.avatar,
        message = message ?: "",
        createdAt = createdAt
    )
}

/**
 * Map API IssueCount to domain IssueCount.
 */
fun ApiIssueCount.toDomain(): IssueCount {
    return IssueCount(
        total = total,
        video = video,
        audio = audio,
        subtitles = subtitles,
        others = others,
        open = open,
        closed = closed
    )
}
