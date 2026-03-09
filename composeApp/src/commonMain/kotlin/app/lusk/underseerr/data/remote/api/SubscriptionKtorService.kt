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
    val isTrial: Boolean = false,
    val expiresAt: Long? = null,
    val trialExpiresAt: Long? = null
)

@Serializable
data class StartTrialRequest(val userId: String)

/**
 * Service for interacting with the Underseerr Subscription/Validation backend.
 */
class SubscriptionKtorService(
    private val client: HttpClient,
    baseUrl: String
) {
    // Strip trailing slash to avoid double-slash in URL concatenation
    private val baseUrl: String = baseUrl.trimEnd('/')
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

    suspend fun startTrial(userId: String): SubscriptionResponse {
        return client.post("$baseUrl/start-trial") {
            contentType(ContentType.Application.Json)
            setBody(StartTrialRequest(userId))
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
