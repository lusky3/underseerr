package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.model.SubscriptionStatus
import app.lusk.underseerr.domain.model.SubscriptionTier
import app.lusk.underseerr.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of SubscriptionRepository using DataStore for mock persistence.
 */
class SubscriptionRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : SubscriptionRepository {

    override fun getSubscriptionStatus(): Flow<SubscriptionStatus> {
        return preferencesManager.getIsPremium().map { isPremium ->
            SubscriptionStatus(
                tier = if (isPremium) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
            )
        }
    }

    override suspend fun refreshSubscriptionStatus(): Result<SubscriptionStatus> {
        // In a real app, this would check an external billing API.
        // For now, it just returns what's in preferences.
        return Result.success(SubscriptionStatus())
    }

    override suspend fun purchasePremium(): Result<Unit> {
        preferencesManager.setIsPremium(true)
        return Result.success(Unit)
    }

    override suspend fun resetSubscription(): Result<Unit> {
        preferencesManager.setIsPremium(false)
        return Result.success(Unit)
    }
}
