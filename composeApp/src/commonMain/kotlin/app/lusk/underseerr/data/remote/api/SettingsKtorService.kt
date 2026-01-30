package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiWebPushGlobalSettings
import app.lusk.underseerr.data.remote.model.ApiUserNotificationSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor implementation of global settings endpoints.
 */
class SettingsKtorService(private val client: HttpClient) {
    
    suspend fun getGlobalWebPushSettings(): ApiWebPushGlobalSettings {
        return client.get("/api/v1/settings/notifications/webpush").body()
    }

    suspend fun updateWebhookSettings(settings: Any) {
        client.post("/api/v1/settings/notifications/webhook") {
             contentType(ContentType.Application.Json)
             setBody(settings)
        }
    }

    suspend fun getUserNotificationSettings(userId: Int): ApiUserNotificationSettings {
        return client.get("/api/v1/user/$userId/settings/notifications").body()
    }
}
