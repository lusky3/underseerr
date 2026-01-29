package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiRegisterPushSubscription
import app.lusk.underseerr.data.remote.model.ApiRequestQuota
import app.lusk.underseerr.data.remote.model.ApiUserNotificationSettings
import app.lusk.underseerr.data.remote.model.ApiUserProfile
import app.lusk.underseerr.data.remote.model.ApiUserStatistics
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

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

    suspend fun registerPushSubscription(subscription: ApiRegisterPushSubscription) {
        client.post("/api/v1/user/registerPushSubscription") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(subscription)
        }
    }

    suspend fun getUserNotificationSettings(userId: Int): ApiUserNotificationSettings {
        return client.get("/api/v1/user/$userId/settings/notifications").body<ApiUserNotificationSettings>()
    }

    suspend fun updateUserNotificationSettings(userId: Int, settings: ApiUserNotificationSettings) {
        client.post("/api/v1/user/$userId/settings/notifications") {
            setBody<ApiUserNotificationSettings>(settings)
        }
    }
}
