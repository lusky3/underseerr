package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiCreateIssueRequest
import app.lusk.underseerr.data.remote.model.ApiIssue
import app.lusk.underseerr.data.remote.model.ApiIssueComment
import app.lusk.underseerr.data.remote.model.ApiIssueCount
import app.lusk.underseerr.data.remote.model.ApiIssueListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Ktor implementation of Issue API endpoints.
 */
class IssueKtorService(private val client: HttpClient) : IssueService {
    
    override suspend fun getIssues(
        take: Int,
        skip: Int,
        sort: String,
        filter: String
    ): ApiIssueListResponse {
        return client.get("/api/v1/issue") {
            parameter("take", take)
            parameter("skip", skip)
            parameter("sort", sort)
            parameter("filter", filter)
        }.body()
    }
    
    override suspend fun getIssueCounts(): ApiIssueCount {
        return client.get("/api/v1/issue/count").body()
    }
    
    override suspend fun getIssue(issueId: Int): ApiIssue {
        return client.get("/api/v1/issue/$issueId").body()
    }
    
    override suspend fun createIssue(request: ApiCreateIssueRequest): ApiIssue {
        return client.post("/api/v1/issue") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun addComment(issueId: Int, message: String): ApiIssue {
        return client.post("/api/v1/issue/$issueId/comment") {
            contentType(ContentType.Application.Json)
            setBody(CommentRequest(message))
        }.body()
    }
    
    override suspend fun deleteIssue(issueId: Int) {
        client.delete("/api/v1/issue/$issueId")
    }
    
    override suspend fun updateIssueStatus(issueId: Int, status: String): ApiIssue {
        return client.post("/api/v1/issue/$issueId/$status").body()
    }

    override suspend fun updateComment(commentId: Int, message: String): ApiIssueComment {
        return client.put("/api/v1/issueComment/$commentId") {
            contentType(ContentType.Application.Json)
            setBody(CommentRequest(message))
        }.body()
    }
}

@Serializable
private data class CommentRequest(val message: String)
