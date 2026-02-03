package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.Result

/**
 * Repository interface for Issue operations.
 */
interface IssueRepository {
    
    /**
     * Get all issues with optional filtering.
     * @param take Number of issues to return
     * @param skip Number of issues to skip for pagination
     * @param filter Filter: "all", "open", or "resolved"
     */
    suspend fun getIssues(
        take: Int = 20,
        skip: Int = 0,
        filter: String = "open"
    ): Result<List<Issue>>
    
    /**
     * Get issue counts by type and status.
     */
    suspend fun getIssueCounts(): Result<IssueCount>
    
    /**
     * Get a single issue by ID.
     */
    suspend fun getIssue(issueId: Int): Result<Issue>
    
    /**
     * Create a new issue.
     * @param issueType 1=video, 2=audio, 3=subtitles, 4=other
     * @param message Initial comment message
     * @param mediaId The media ID to create the issue for
     */
    suspend fun createIssue(
        issueType: Int,
        message: String,
        mediaId: Int,
        problemSeason: Int = 0,
        problemEpisode: Int = 0
    ): Result<Issue>
    
    /**
     * Add a comment to an issue.
     */
    suspend fun addComment(issueId: Int, message: String): Result<Issue>
    
    /**
     * Delete an issue.
     */
    suspend fun deleteIssue(issueId: Int): Result<Unit>
    
    /**
     * Resolve an issue.
     */
    suspend fun resolveIssue(issueId: Int): Result<Issue>
    
    /**
     * Reopen an issue.
     */
    suspend fun reopenIssue(issueId: Int): Result<Issue>

    /**
     * Update an issue comment.
     */
    suspend fun updateComment(commentId: Int, message: String): Result<app.lusk.underseerr.domain.model.IssueComment>
}
