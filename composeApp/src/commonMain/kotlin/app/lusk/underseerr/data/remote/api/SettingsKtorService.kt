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
}
