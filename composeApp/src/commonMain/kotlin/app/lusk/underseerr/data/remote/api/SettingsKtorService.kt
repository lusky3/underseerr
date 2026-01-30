package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiWebPushGlobalSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Ktor implementation of global settings endpoints.
 */
class SettingsKtorService(private val client: HttpClient) {
    
    suspend fun getGlobalWebPushSettings(): ApiWebPushGlobalSettings {
        return client.get("/api/v1/settings/notifications/webpush").body()
    }

    suspend fun updateWebhookSettings(settings: Map<String, Any?>) {
        client.post("/api/v1/settings/notifications/webhook") {
             io.ktor.http.contentType(io.ktor.http.ContentType.Application.Json)
             io.ktor.client.request.setBody(settings)
        }
    }
}
