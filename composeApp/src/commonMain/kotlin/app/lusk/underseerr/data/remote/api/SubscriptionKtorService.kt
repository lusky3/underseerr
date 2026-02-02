package app.lusk.underseerr.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class SerialKeyRequest(val key: String, val userId: String)

@Serializable
data class SubscriptionResponse(
    val isPremium: Boolean,
    val expiresAt: Long? = null
)

/**
 * Service for interacting with the Underseerr Subscription/Validation backend.
 */
class SubscriptionKtorService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun validateSerialKey(key: String, userId: String): SubscriptionResponse {
        return client.post("$baseUrl/validate-key") {
            contentType(ContentType.Application.Json)
            setBody(SerialKeyRequest(key, userId))
        }.body()
    }

    suspend fun checkSubscriptionStatus(userId: String): SubscriptionResponse {
        return client.get("$baseUrl/subscription-status") {
            parameter("userId", userId)
        }.body()
    }

    suspend fun verifyPurchase(userId: String, details: app.lusk.underseerr.domain.billing.PurchaseDetails): SubscriptionResponse {
        return client.post("$baseUrl/verify-purchase") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "userId" to userId,
                "productId" to details.productId,
                "purchaseToken" to details.purchaseToken,
                "packageName" to details.packageName
            ))
        }.body()
    }
}
