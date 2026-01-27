package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiCreateIssueRequest
import app.lusk.underseerr.data.remote.model.ApiIssue
import app.lusk.underseerr.data.remote.model.ApiIssueComment
import app.lusk.underseerr.data.remote.model.ApiIssueCount
import app.lusk.underseerr.data.remote.model.ApiIssueListResponse

/**
 * Service interface for Issue-related API operations.
 */
interface IssueService {
    
    /**
     * Get all issues with optional filtering and pagination.
     * @param take Number of issues to return
     * @param skip Number of issues to skip
     * @param sort Sort order: "added" or "modified"
     * @param filter Filter: "all", "open", or "resolved"
     */
    suspend fun getIssues(
        take: Int = 20,
        skip: Int = 0,
        sort: String = "added",
        filter: String = "open"
    ): ApiIssueListResponse
    
    /**
     * Get issue counts by type and status.
     */
    suspend fun getIssueCounts(): ApiIssueCount
    
    /**
     * Get a single issue by ID.
     */
    suspend fun getIssue(issueId: Int): ApiIssue
    
    /**
     * Create a new issue.
     */
    suspend fun createIssue(request: ApiCreateIssueRequest): ApiIssue
    
    /**
     * Add a comment to an issue.
     */
    suspend fun addComment(issueId: Int, message: String): ApiIssue
    
    /**
     * Delete an issue.
     */
    suspend fun deleteIssue(issueId: Int)
    
    /**
     * Update issue status.
     * @param status "open" or "resolved"
     */
    suspend fun updateIssueStatus(issueId: Int, status: String): ApiIssue
    /**
     * Update an issue comment.
     */
    suspend fun updateComment(commentId: Int, message: String): ApiIssueComment
}
