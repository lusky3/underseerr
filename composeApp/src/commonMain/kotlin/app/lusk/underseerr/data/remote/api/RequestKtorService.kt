package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*

/**
 * Interface for media request endpoints.
 */
interface RequestKtorService {
    suspend fun getSystemSettings(): ApiSystemSettings
    
    suspend fun submitRequest(body: ApiRequestBody): ApiMediaRequest
    
    suspend fun getRequests(
        take: Int = 20,
        skip: Int = 0,
        filter: String = "all",
        sort: String = "added",
        requestedBy: Int? = null
    ): RequestsResponse
    
    suspend fun getRequest(requestId: Int): ApiMediaRequest
    
    suspend fun deleteRequest(requestId: Int)
    
    suspend fun getUserRequests(
        userId: Int,
        take: Int = 20,
        skip: Int = 0
    ): RequestsResponse
    
    suspend fun getRequestStatus(requestId: Int): ApiRequestStatus
    
    suspend fun getRadarrServers(): List<ApiMediaServer>
    
    suspend fun getSonarrServers(): List<ApiMediaServer>
    
    suspend fun getRadarrService(id: Int): ApiServiceSettings
    
    suspend fun getSonarrService(id: Int): ApiServiceSettings
    
    suspend fun approveRequest(requestId: Int): ApiMediaRequest
    
    suspend fun declineRequest(requestId: Int)
}

/**
 * Ktor implementation of media request endpoints.
 */
open class RequestServiceImpl(private val client: HttpClient) : RequestKtorService {
    
    override suspend fun getSystemSettings(): ApiSystemSettings {
        return client.get("/api/v1/settings/main").body()
    }
    
    override suspend fun submitRequest(body: ApiRequestBody): ApiMediaRequest {
        return client.post("/api/v1/request") {
            setBody(body)
        }.body()
    }
    
    override suspend fun getRequests(
        take: Int,
        skip: Int,
        filter: String,
        sort: String,
        requestedBy: Int?
    ): RequestsResponse {
        return client.get("/api/v1/request") {
            parameter("take", take)
            parameter("skip", skip)
            parameter("filter", filter)
            parameter("sort", sort)
            requestedBy?.let { parameter("requestedBy", it) }
        }.body()
    }
    
    override suspend fun getRequest(requestId: Int): ApiMediaRequest {
        return client.get("/api/v1/request/$requestId").body()
    }
    
    override suspend fun deleteRequest(requestId: Int) {
        client.delete("/api/v1/request/$requestId")
    }
    
    override suspend fun getUserRequests(
        userId: Int,
        take: Int,
        skip: Int
    ): RequestsResponse {
        return client.get("/api/v1/user/$userId/requests") {
            parameter("take", take)
            parameter("skip", skip)
        }.body()
    }
    
    override suspend fun getRequestStatus(requestId: Int): ApiRequestStatus {
        return client.get("/api/v1/request/$requestId/status").body()
    }
    
    override suspend fun getRadarrServers(): List<ApiMediaServer> {
        return client.get("/api/v1/service/radarr").body()
    }

    override suspend fun getSonarrServers(): List<ApiMediaServer> {
        return client.get("/api/v1/service/sonarr").body()
    }
    
    override suspend fun getRadarrService(id: Int): ApiServiceSettings {
        return client.get("/api/v1/service/radarr/$id").body()
    }

    override suspend fun getSonarrService(id: Int): ApiServiceSettings {
        return client.get("/api/v1/service/sonarr/$id").body()
    }
    
    override suspend fun approveRequest(requestId: Int): ApiMediaRequest {
        return client.post("/api/v1/request/$requestId/approve").body()
    }
    
    override suspend fun declineRequest(requestId: Int) {
        client.post("/api/v1/request/$requestId/decline")
    }
}
