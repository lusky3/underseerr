package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.remote.api.IssueService
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.data.remote.model.ApiCreateIssueRequest
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueComment
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.repository.IssueRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Implementation of IssueRepository using IssueService.
 */
class IssueRepositoryImpl(
    private val issueService: IssueService,
    private val discoveryService: DiscoveryKtorService
) : IssueRepository {
    
    override suspend fun getIssues(
        take: Int,
        skip: Int,
        filter: String
    ): Result<List<Issue>> = runCatching {
        val response = issueService.getIssues(
            take = take,
            skip = skip,
            sort = "added",
            filter = filter
        )
        val issues = response.results.map { it.toDomain() }

        coroutineScope {
            issues.map { issue ->
                async {
                    if (issue.mediaTitle.startsWith("Movie (TMDB:") || issue.mediaTitle.startsWith("TV Show (TMDB:")) {
                        val tmdbId = issue.mediaTmdbId
                        if (tmdbId != null) {
                            try {
                                val title = if (issue.mediaType == MediaType.MOVIE) {
                                    discoveryService.getMovieDetails(tmdbId).title
                                } else {
                                    discoveryService.getTvShowDetails(tmdbId).name
                                }
                                issue.copy(mediaTitle = title)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                issue
                            }
                        } else {
                            issue
                        }
                    } else {
                        issue
                    }
                }
            }.awaitAll()
        }
    }
    
    override suspend fun getIssueCounts(): Result<IssueCount> = runCatching {
        issueService.getIssueCounts().toDomain()
    }
    
    override suspend fun getIssue(issueId: Int): Result<Issue> = runCatching {
        val issue = issueService.getIssue(issueId).toDomain()

        if (issue.mediaTitle.startsWith("Movie (TMDB:") || issue.mediaTitle.startsWith("TV Show (TMDB:")) {
            val tmdbId = issue.mediaTmdbId
            if (tmdbId != null) {
                try {
                    val title = if (issue.mediaType == MediaType.MOVIE) {
                        discoveryService.getMovieDetails(tmdbId).title
                    } else {
                        discoveryService.getTvShowDetails(tmdbId).name
                    }
                    issue.copy(mediaTitle = title)
                } catch (e: Exception) {
                    e.printStackTrace()
                    issue
                }
            } else {
                issue
            }
        } else {
            issue
        }
    }
    
    override suspend fun createIssue(
        issueType: Int,
        message: String,
        mediaId: Int,
        problemSeason: Int,
        problemEpisode: Int
    ): Result<Issue> = runCatching {
        val request = ApiCreateIssueRequest(
            issueType = issueType,
            message = message,
            mediaId = mediaId,
            problemSeason = problemSeason,
            problemEpisode = problemEpisode
        )
        issueService.createIssue(request).toDomain()
    }
    
    override suspend fun addComment(issueId: Int, message: String): Result<Issue> = runCatching {
        issueService.addComment(issueId, message).toDomain()
    }
    
    override suspend fun deleteIssue(issueId: Int): Result<Unit> = runCatching {
        issueService.deleteIssue(issueId)
    }
    
    override suspend fun resolveIssue(issueId: Int): Result<Issue> = runCatching {
        issueService.updateIssueStatus(issueId, "resolved").toDomain()
    }
    
    override suspend fun reopenIssue(issueId: Int): Result<Issue> = runCatching {
        issueService.updateIssueStatus(issueId, "open").toDomain()
    }

    override suspend fun updateComment(commentId: Int, message: String): Result<IssueComment> = runCatching {
        issueService.updateComment(commentId, message).toDomain()
    }
}
