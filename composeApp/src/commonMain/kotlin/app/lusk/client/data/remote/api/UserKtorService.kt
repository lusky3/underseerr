package app.lusk.client.data.remote.api

import app.lusk.client.data.remote.model.ApiRequestQuota
import app.lusk.client.data.remote.model.ApiUserProfile
import app.lusk.client.data.remote.model.ApiUserStatistics
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Ktor implementation of user profile endpoints.
 */
class UserKtorService(private val client: HttpClient) {
    
    suspend fun getUserProfile(userId: Int): ApiUserProfile {
        return client.get("/api/v1/user/$userId").body()
    }
    
    suspend fun getCurrentUser(): ApiUserProfile {
        return client.get("/api/v1/auth/me").body()
    }
    
    suspend fun getUserQuota(userId: Int): ApiRequestQuota {
        return client.get("/api/v1/user/$userId/quota").body()
    }
    
    suspend fun getUserStatistics(userId: Int): ApiUserStatistics {
        return client.get("/api/v1/user/$userId/stats").body()
    }
}
