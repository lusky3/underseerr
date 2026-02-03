package app.lusk.underseerr.domain.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for platform-specific billing implementations.
 */
@kotlinx.serialization.Serializable
data class PurchaseDetails(
    val productId: String,
    val purchaseToken: String,
    val packageName: String
)

interface BillingManager {
    /**
     * Start the billing connection.
     */
    fun startConnection()

    /**
     * Purchase a premium product.
     * @param productId The ID of the product to purchase.
     * @param basePlanId Optional ID of the specific base plan (e.g., monthly vs yearly).
     */
    suspend fun purchaseProduct(productId: String, basePlanId: String? = null): Result<Unit>

    /**
     * Check if the user has an active subscription.
     */
    suspend fun isSubscribed(productId: String): Boolean
    
    /**
     * Provides the current subscription status as a flow.
     */
    val isSubscribed: StateFlow<Boolean>

    /**
     * Flow of new successful purchases that need server-side verification.
     */
    val purchaseDetails: kotlinx.coroutines.flow.SharedFlow<PurchaseDetails>
}
