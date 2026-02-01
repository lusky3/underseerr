package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user subscription state.
 */
interface SubscriptionRepository {
    /**
     * Get the current subscription status as a flow.
     */
    fun getSubscriptionStatus(): Flow<SubscriptionStatus>

    /**
     * Refreshes the subscription status from a remote source (or mock).
     */
    suspend fun refreshSubscriptionStatus(): Result<SubscriptionStatus>

    /**
     * Initiates a mock purchase for testing purposes.
     */
    suspend fun purchasePremium(): Result<Unit>

    /**
     * Resets subscription to free (for testing).
     */
    suspend fun resetSubscription(): Result<Unit>
}
