package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.toAppError
import app.lusk.underseerr.data.remote.api.IssueService
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.data.remote.model.ApiCreateIssueRequest
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueComment
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.data.remote.safeApiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Implementation of IssueRepository using IssueService.
 */
class IssueRepositoryImpl(
    private val issueService: IssueService,
    private val discoveryService: DiscoveryKtorService,
    private val issueDao: app.lusk.underseerr.data.local.dao.IssueDao
) : IssueRepository {
    
    override suspend fun getIssues(
        take: Int,
        skip: Int,
        filter: String
    ): Result<List<Issue>> {
        // Try network first
        val result = try {
            val response = issueService.getIssues(
                take = take,
                skip = skip,
                sort = "added",
                filter = filter
            )
            val issues = response.results.map { it.toDomain() }
            
            // Hydrate and Cache
            val hydratedIssues = coroutineScope {
                issues.map { issue ->
                    async {
                        hydrateIssue(issue)
                    }
                }.awaitAll()
            }
            
            // Cache in background
            try {
                issueDao.insertIssues(hydratedIssues.map { it.toEntity() })
            } catch (e: Exception) {
                // Ignore cache errors
            }
            
            Result.success(hydratedIssues)
        } catch (e: Exception) {
            // Fallback to cache if network fails
            try {
                val cached = issueDao.getAllIssuesSync().map { it.toDomain() }
                
                // If we have cached items and user wants 'open' or 'resolved' specifically, filter issues locally
                val filtered = when (filter) {
                    "open" -> cached.filter { it.status == app.lusk.underseerr.domain.model.IssueStatus.OPEN }
                    "resolved" -> cached.filter { it.status == app.lusk.underseerr.domain.model.IssueStatus.RESOLVED }
                    else -> cached
                }
                
                if (filtered.isNotEmpty()) {
                    Result.success(filtered)
                } else {
                    Result.error(e.toAppError())
                }
            } catch (cacheError: Exception) {
                Result.error(e.toAppError())
            }
        }
        
        return result
    }
    
    private suspend fun hydrateIssue(issue: Issue): Issue {
        if (issue.mediaTitle.startsWith("Movie (TMDB:") || issue.mediaTitle.startsWith("TV Show (TMDB:")) {
            val tmdbId = issue.mediaTmdbId
            if (tmdbId != null) {
                return try {
                    if (issue.mediaType == MediaType.MOVIE) {
                        val details = discoveryService.getMovieDetails(tmdbId)
                        issue.copy(
                            mediaTitle = details.title,
                            mediaPosterPath = details.posterPath ?: issue.mediaPosterPath
                        )
                    } else {
                        val details = discoveryService.getTvShowDetails(tmdbId)
                        issue.copy(
                            mediaTitle = details.name,
                            mediaPosterPath = details.posterPath ?: issue.mediaPosterPath
                        )
                    }
                } catch (e: Exception) {
                    issue
                }
            }
        }
        return issue
    }
    
    override suspend fun getIssueCounts(): Result<IssueCount> {
        return try {
            val apiCounts = issueService.getIssueCounts().toDomain()
            Result.success(apiCounts)
        } catch (e: Exception) {
            // Fallback: Calculate counts from local DB
            try {
                val allIssues = issueDao.getAllIssuesSync()
                val openCount = allIssues.count { it.status == 1 } // OPEN
                val resolvedCount = allIssues.count { it.status == 2 } // RESOLVED
                
                val counts = IssueCount(
                    total = allIssues.size,
                    video = allIssues.count { it.issueType == 1 },
                    audio = allIssues.count { it.issueType == 2 },
                    subtitles = allIssues.count { it.issueType == 3 },
                    others = allIssues.count { it.issueType == 4 },
                    open = openCount,
                    closed = resolvedCount
                )
                Result.success(counts)
            } catch (dbError: Exception) {
                Result.error(e.toAppError())
            }
        }
    }
    
    override suspend fun getIssue(issueId: Int): Result<Issue> = safeApiCall {
        val issue = issueService.getIssue(issueId).toDomain()
        hydrateIssue(issue)
    }
    
    override suspend fun createIssue(
        issueType: Int,
        message: String,
        mediaId: Int,
        problemSeason: Int,
        problemEpisode: Int
    ): Result<Issue> = safeApiCall {
        val request = ApiCreateIssueRequest(
            issueType = issueType,
            message = message,
            mediaId = mediaId,
            problemSeason = problemSeason,
            problemEpisode = problemEpisode
        )
        issueService.createIssue(request).toDomain()
    }
    
    override suspend fun addComment(issueId: Int, message: String): Result<Issue> = safeApiCall {
        issueService.addComment(issueId, message).toDomain()
    }
    
    override suspend fun deleteIssue(issueId: Int): Result<Unit> {
        return try {
             issueService.deleteIssue(issueId)
             // Remove from local cache
             try {
                 issueDao.deleteIssue(issueId)
             } catch (e: Exception) { /* ignore */ }
             Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }
    
    override suspend fun resolveIssue(issueId: Int): Result<Issue> {
        return try {
            val issue = issueService.updateIssueStatus(issueId, "resolved").toDomain()
            // Update local cache
            try {
                issueDao.insertIssue(issue.toEntity())
            } catch (e: Exception) { /* ignore */ }
            Result.success(issue)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }
    
    override suspend fun reopenIssue(issueId: Int): Result<Issue> {
        return try {
            val issue = issueService.updateIssueStatus(issueId, "open").toDomain()
            // Update local cache
            try {
                issueDao.insertIssue(issue.toEntity())
            } catch (e: Exception) { /* ignore */ }
            Result.success(issue)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }
    
    override suspend fun updateComment(commentId: Int, message: String): Result<IssueComment> = safeApiCall {
        issueService.updateComment(commentId, message).toDomain()
    }
}
