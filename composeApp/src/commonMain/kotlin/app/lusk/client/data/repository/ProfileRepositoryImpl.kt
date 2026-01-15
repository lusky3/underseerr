package app.lusk.client.data.repository

import app.lusk.client.data.remote.api.UserKtorService
import app.lusk.client.data.mapper.toDomain
import app.lusk.client.data.remote.safeApiCall
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.model.UserStatistics
import app.lusk.client.domain.repository.ProfileRepository
import app.lusk.client.domain.repository.RequestQuota

/**
 * Implementation of ProfileRepository.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.1
 */
class ProfileRepositoryImpl(
    private val userKtorService: UserKtorService,
    private val mediaRequestDao: app.lusk.client.data.local.dao.MediaRequestDao
) : ProfileRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> = safeApiCall {
        val apiProfile = userKtorService.getCurrentUser()
        apiProfile.toDomain()
    }
    
    override suspend fun getUserQuota(): Result<RequestQuota> = safeApiCall {
        val user = userKtorService.getCurrentUser()
        val apiQuota = userKtorService.getUserQuota(user.id)
        
        RequestQuota(
            movieLimit = apiQuota.movie?.limit,
            movieRemaining = apiQuota.movie?.remaining,
            movieDays = apiQuota.movie?.days,
            tvLimit = apiQuota.tv?.limit,
            tvRemaining = apiQuota.tv?.remaining,
            tvDays = apiQuota.tv?.days
        )
    }
    
    override suspend fun getUserStatistics(): Result<UserStatistics> = safeApiCall {
        val user = userKtorService.getCurrentUser()
        
        // Use local database for statistics since /user/{id}/stats endpoint is 404
        val allRequests = mediaRequestDao.getAllSync()

        val approved = allRequests.count { it.status == 2 }
        val declined = allRequests.count { it.status == 3 }
        val pending = allRequests.count { it.status == 1 }
        val available = allRequests.count { it.status == 4 || it.status == 5 }

        UserStatistics(
            totalRequests = if (user.requestCount > 0) user.requestCount else allRequests.size,
            approvedRequests = approved,
            declinedRequests = declined,
            pendingRequests = pending,
            availableRequests = available
        )
    }
}
