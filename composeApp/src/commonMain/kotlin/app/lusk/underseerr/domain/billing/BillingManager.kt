package app.lusk.underseerr.domain.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for platform-specific billing implementations.
 */
interface BillingManager {
    /**
     * Start the billing connection.
     */
    fun startConnection()

    /**
     * Purchase a premium product.
     * @param productId The ID of the product to purchase.
     */
    suspend fun purchaseProduct(productId: String): Result<Unit>

    /**
     * Check if the user has an active subscription.
     */
    suspend fun isSubscribed(productId: String): Boolean
    
    /**
     * Provides the current subscription status as a flow.
     */
    val isSubscribed: StateFlow<Boolean>
}
