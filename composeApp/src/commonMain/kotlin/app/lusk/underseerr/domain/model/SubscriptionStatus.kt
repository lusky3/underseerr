package app.lusk.underseerr.domain.model

import kotlinx.serialization.Serializable

/**
 * Tiers of subscription available.
 */
enum class SubscriptionTier {
    FREE,
    TRIAL,
    PREMIUM,

    /** Free for everyone for a limited period, granted by the server. May end early. */
    COMPLIMENTARY
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

    /** Whether the hosted notification relay is available: any tier except FREE. */
    val hasHostedAccess: Boolean get() = tier != SubscriptionTier.FREE
}
