package app.lusk.client.data.remote.api

import app.lusk.client.data.remote.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor implementation of media request endpoints.
 */
class RequestKtorService(private val client: HttpClient) {
    
    suspend fun getSystemSettings(): ApiSystemSettings {
        return client.get("/api/v1/settings/main").body()
    }
    
    suspend fun submitRequest(body: ApiRequestBody): ApiMediaRequest {
        return client.post("/api/v1/request") {
            setBody(body)
        }.body()
    }
    
    suspend fun getRequests(
        take: Int = 20,
        skip: Int = 0,
        filter: String = "all",
        sort: String = "added",
        requestedBy: Int? = null
    ): RequestsResponse {
        return client.get("/api/v1/request") {
            parameter("take", take)
            parameter("skip", skip)
            parameter("filter", filter)
            parameter("sort", sort)
            requestedBy?.let { parameter("requestedBy", it) }
        }.body()
    }
    
    suspend fun getRequest(requestId: Int): ApiMediaRequest {
        return client.get("/api/v1/request/$requestId").body()
    }
    
    suspend fun deleteRequest(requestId: Int) {
        client.delete("/api/v1/request/$requestId")
    }
    
    suspend fun getUserRequests(
        userId: Int,
        take: Int = 20,
        skip: Int = 0
    ): RequestsResponse {
        return client.get("/api/v1/user/$userId/requests") {
            parameter("take", take)
            parameter("skip", skip)
        }.body()
    }
    
    suspend fun getRequestStatus(requestId: Int): ApiRequestStatus {
        return client.get("/api/v1/request/$requestId/status").body()
    }
    
    suspend fun getRadarrServers(): List<ApiMediaServer> {
        return client.get("/api/v1/service/radarr").body()
    }

    suspend fun getSonarrServers(): List<ApiMediaServer> {
        return client.get("/api/v1/service/sonarr").body()
    }
    
    suspend fun getRadarrService(id: Int): ApiServiceSettings {
        return client.get("/api/v1/service/radarr/$id").body()
    }

    suspend fun getSonarrService(id: Int): ApiServiceSettings {
        return client.get("/api/v1/service/sonarr/$id").body()
    }
}
