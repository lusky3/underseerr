package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.model.SubscriptionStatus
import app.lusk.underseerr.domain.model.SubscriptionTier
import app.lusk.underseerr.domain.repository.SubscriptionRepository
import app.lusk.underseerr.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Implementation of SubscriptionRepository using DataStore for mock persistence.
 */
class SubscriptionRepositoryImpl(
    private val preferencesManager: PreferencesManager,
    private val subscriptionKtorService: app.lusk.underseerr.data.remote.api.SubscriptionKtorService,
    private val authRepository: app.lusk.underseerr.domain.repository.AuthRepository,
    private val billingManager: app.lusk.underseerr.domain.billing.BillingManager
) : SubscriptionRepository {

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    init {
        scope.launch {
            billingManager.isSubscribed.collect { isSubscribed ->
                if (isSubscribed) {
                    preferencesManager.setIsPremium(true)
                }
            }
        }
    }

    override fun getSubscriptionStatus(): Flow<SubscriptionStatus> {
        return combine(
            preferencesManager.getIsPremium(),
            preferencesManager.getTrialStartDate(),
            preferencesManager.getPremiumExpiresAt()
        ) { isPremium, trialStartDate, premiumExpiresAt ->
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
                
                preferencesManager.setIsPremium(response.isPremium)
                preferencesManager.setPremiumExpiresAt(response.expiresAt)
                
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

    override suspend fun purchasePremium(): Result<Unit> {
        return billingManager.purchaseProduct("premium_subscription")
    }

    override suspend fun resetSubscription(): Result<Unit> {
        preferencesManager.setIsPremium(false)
        preferencesManager.setTrialStartDate(null)
        preferencesManager.setPremiumExpiresAt(null)
        return Result.success(Unit)
    }

    override suspend fun unlockWithSerialKey(key: String): Result<Unit> {
        return try {
            val response = subscriptionKtorService.validateSerialKey(key)
            if (response.isPremium) {
                preferencesManager.setIsPremium(true)
                preferencesManager.setPremiumExpiresAt(response.expiresAt)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid serial key"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        return try {
            val isPremium = billingManager.isSubscribed("premium_subscription")
            preferencesManager.setIsPremium(isPremium)
            if (isPremium) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No active subscription found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
