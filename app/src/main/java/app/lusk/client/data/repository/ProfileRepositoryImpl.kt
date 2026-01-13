package app.lusk.client.data.repository

import app.lusk.client.data.remote.api.UserApiService
import app.lusk.client.data.mapper.toDomain
import app.lusk.client.data.remote.safeApiCall
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.model.UserStatistics
import app.lusk.client.domain.repository.ProfileRepository
import app.lusk.client.domain.repository.RequestQuota
import javax.inject.Inject

/**
 * Implementation of ProfileRepository.
 * Feature: overseerr-android-client
 * Validates: Requirements 5.1
 */
class ProfileRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val mediaRequestDao: app.lusk.client.data.local.dao.MediaRequestDao
) : ProfileRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> = safeApiCall {
        val apiProfile = userApiService.getCurrentUser()
        apiProfile.toDomain()
    }
    
    override suspend fun getUserQuota(): Result<RequestQuota> = safeApiCall {
        val user = userApiService.getCurrentUser()
        val apiQuota = userApiService.getUserQuota(user.id)
        
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
        val user = userApiService.getCurrentUser()
        
        // Try actual stats first
        try {
            val apiStats = userApiService.getUserStatistics(user.id)
            UserStatistics(
                totalRequests = apiStats.totalRequests,
                approvedRequests = apiStats.approvedRequests,
                declinedRequests = apiStats.declinedRequests,
                pendingRequests = apiStats.pendingRequests,
                availableRequests = apiStats.availableRequests
            )
        } catch (e: Exception) {
            // Fallback: calculate from local database which contains synced requests
            val allRequests = mediaRequestDao.getAllSync()
            
            UserStatistics(
                totalRequests = user.requestCount,
                approvedRequests = allRequests.count { it.status == 2 }, // APPROVED
                declinedRequests = allRequests.count { it.status == 3 }, // DECLINED
                pendingRequests = allRequests.count { it.status == 1 }, // PENDING
                availableRequests = allRequests.count { it.status == 4 || it.status == 5 } // AVAILABLE
            )
        }
    }
}
