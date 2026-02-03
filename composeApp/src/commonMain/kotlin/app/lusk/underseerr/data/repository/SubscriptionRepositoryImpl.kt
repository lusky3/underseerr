package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.model.SubscriptionStatus
import app.lusk.underseerr.domain.model.SubscriptionTier
import app.lusk.underseerr.domain.repository.SubscriptionRepository
import app.lusk.underseerr.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Implementation of SubscriptionRepository using DataStore for mock persistence.
 */
class SubscriptionRepositoryImpl(
    private val preferencesManager: PreferencesManager,
    private val securityManager: app.lusk.underseerr.domain.security.SecurityManager,
    private val subscriptionKtorService: app.lusk.underseerr.data.remote.api.SubscriptionKtorService,
    private val authRepository: app.lusk.underseerr.domain.repository.AuthRepository,
    private val billingManager: app.lusk.underseerr.domain.billing.BillingManager
) : SubscriptionRepository {

    private companion object {
        const val SECURE_IS_PREMIUM = "underseerr_is_premium"
        const val SECURE_PREMIUM_EXPIRES_AT = "underseerr_premium_expires_at"
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    init {
        scope.launch {
            billingManager.isSubscribed.collect { isSubscribed ->
                if (isSubscribed) {
                    setPremiumSecure(true)
                }
            }
        }

        scope.launch {
            billingManager.purchaseDetails.collect { details ->
                val userResult = authRepository.getCurrentUser()
                if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
                    try {
                        val response = subscriptionKtorService.verifyPurchase(userResult.data.id.toString(), details)
                        if (response.isPremium) {
                            setPremiumSecure(true, response.expiresAt)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun setPremiumSecure(isPremium: Boolean, expiresAt: Long? = null) {
        securityManager.storeSecureData(SECURE_IS_PREMIUM, isPremium.toString())
        if (expiresAt != null) {
            securityManager.storeSecureData(SECURE_PREMIUM_EXPIRES_AT, expiresAt.toString())
        } else {
            securityManager.storeSecureData(SECURE_PREMIUM_EXPIRES_AT, "")
        }
        // Also update preferences for UI flow
        preferencesManager.setIsPremium(isPremium)
        preferencesManager.setPremiumExpiresAt(expiresAt)
    }

    private suspend fun getIsPremiumSecure(): Boolean {
        return securityManager.retrieveSecureData(SECURE_IS_PREMIUM)?.toBoolean() ?: false
    }

    private suspend fun getPremiumExpiresAtSecure(): Long? {
        return securityManager.retrieveSecureData(SECURE_PREMIUM_EXPIRES_AT)?.toLongOrNull()
    }

    override fun getSubscriptionStatus(): Flow<SubscriptionStatus> {
        return combine(
            preferencesManager.getIsPremium(),
            preferencesManager.getTrialStartDate(),
            preferencesManager.getPremiumExpiresAt()
        ) { isPremium, trialStartDate, premiumExpiresAt ->
            // In a production app, we could double check against SecurityManager here,
            // but for reactive Flow, DataStore is sufficient for UI.
            val now = nowMillis()
            val trialDuration = 7L * 24 * 60 * 60 * 1000 // 7 days in ms
            
            when {
                isPremium -> SubscriptionStatus(
                    tier = SubscriptionTier.PREMIUM,
                    expiresAt = premiumExpiresAt
                )
                trialStartDate != null && (now - trialStartDate) < trialDuration -> {
                    SubscriptionStatus(
                        tier = SubscriptionTier.TRIAL,
                        expiresAt = trialStartDate + trialDuration
                    )
                }
                else -> SubscriptionStatus(tier = SubscriptionTier.FREE)
            }
        }
    }

    override suspend fun refreshSubscriptionStatus(): Result<SubscriptionStatus> {
        return try {
            val userResult = authRepository.getCurrentUser()
            if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
                val userId = userResult.data.id.toString()
                val response = subscriptionKtorService.checkSubscriptionStatus(userId)
                
                setPremiumSecure(response.isPremium, response.expiresAt)
                
                Result.success(SubscriptionStatus(
                    tier = if (response.isPremium) SubscriptionTier.PREMIUM else SubscriptionTier.FREE,
                    expiresAt = response.expiresAt
                ))
            } else {
                Result.failure(Exception("Not logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchasePremium(isYearly: Boolean): Result<Unit> {
        val basePlanId = if (isYearly) "premium-yearly" else "premium-monthly"
        return billingManager.purchaseProduct("premium_subscription", basePlanId)
    }

    override suspend fun resetSubscription(): Result<Unit> {
        setPremiumSecure(false, null)
        preferencesManager.setTrialStartDate(null)
        return Result.success(Unit)
    }

    override suspend fun unlockWithSerialKey(key: String): Result<Unit> {
        return try {
            val userResult = authRepository.getCurrentUser()
            if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
                val userId = userResult.data.id.toString()
                val response = subscriptionKtorService.validateSerialKey(key, userId)
                if (response.isPremium) {
                    setPremiumSecure(true, response.expiresAt)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid serial key"))
                }
            } else {
                Result.failure(Exception("Not logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        return try {
            val isPremium = billingManager.isSubscribed("premium_subscription")
            if (isPremium) {
                setPremiumSecure(true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No active subscription found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
