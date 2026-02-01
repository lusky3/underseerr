package app.lusk.underseerr.domain.model

import kotlinx.serialization.Serializable

/**
 * Tiers of subscription available.
 */
enum class SubscriptionTier {
    FREE,
    TRIAL,
    PREMIUM
}

/**
 * Represents the current subscription status of the user.
 */
@Serializable
data class SubscriptionStatus(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val expiresAt: Long? = null
) {
    val isPremium: Boolean get() = tier == SubscriptionTier.PREMIUM
}
