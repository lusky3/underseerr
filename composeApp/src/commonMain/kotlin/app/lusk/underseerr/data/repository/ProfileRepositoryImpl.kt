package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.remote.api.UserKtorService
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.data.local.entity.UserEntity
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.model.UserStatistics
import app.lusk.underseerr.domain.repository.ProfileRepository
import app.lusk.underseerr.domain.repository.RequestQuota

/**
 * Implementation of ProfileRepository.
 * Feature: underseerr
 * Validates: Requirements 5.1
 */
class ProfileRepositoryImpl(
    private val userKtorService: UserKtorService,
    private val mediaRequestDao: app.lusk.underseerr.data.local.dao.MediaRequestDao,
    private val userDao: app.lusk.underseerr.data.local.dao.UserDao
) : ProfileRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        val remoteResult = safeApiCall {
            userKtorService.getCurrentUser().toDomain()
        }

        if (remoteResult is Result.Success) {
            val profile = remoteResult.data
            // Update cache
            val current = userDao.getUserSync()
            userDao.insert(
                UserEntity(
                    id = profile.id,
                    email = profile.email,
                    displayName = profile.displayName,
                    avatar = profile.avatar,
                    isPlexUser = profile.isPlexUser,
                    permissions = profile.rawPermissions,
                    requestCount = profile.requestCount,
                    movieLimit = current?.movieLimit,
                    movieRemaining = current?.movieRemaining,
                    movieDays = current?.movieDays,
                    tvLimit = current?.tvLimit,
                    tvRemaining = current?.tvRemaining,
                    tvDays = current?.tvDays
                )
            )
            return remoteResult
        }

        // Fallback to cache
        val cached = userDao.getUserSync()
        if (cached != null) {
            return Result.success(cached.toDomain())
        }

        return remoteResult
    }
    
    override suspend fun getUserQuota(): Result<RequestQuota> {
        val user = safeApiCall { userKtorService.getCurrentUser() }
        if (user is Result.Success) {
            val apiQuota = safeApiCall { userKtorService.getUserQuota(user.data.id) }
            if (apiQuota is Result.Success) {
                val quota = RequestQuota(
                    movieLimit = apiQuota.data.movie?.limit,
                    movieRemaining = apiQuota.data.movie?.remaining,
                    movieDays = apiQuota.data.movie?.days,
                    tvLimit = apiQuota.data.tv?.limit,
                    tvRemaining = apiQuota.data.tv?.remaining,
                    tvDays = apiQuota.data.tv?.days
                )
                
                // Update cache
                val current = userDao.getUserSync()
                if (current != null) {
                    userDao.insert(current.copy(
                        movieLimit = quota.movieLimit,
                        movieRemaining = quota.movieRemaining,
                        movieDays = quota.movieDays,
                        tvLimit = quota.tvLimit,
                        tvRemaining = quota.tvRemaining,
                        tvDays = quota.tvDays
                    ))
                }
                
                return Result.success(quota)
            }
        }
        
        // Fallback to cache
        val cached = userDao.getUserSync()
        if (cached != null) {
            return Result.success(
                RequestQuota(
                    movieLimit = cached.movieLimit,
                    movieRemaining = cached.movieRemaining,
                    movieDays = cached.movieDays,
                    tvLimit = cached.tvLimit,
                    tvRemaining = cached.tvRemaining,
                    tvDays = cached.tvDays
                )
            )
        }
        
        return Result.error(app.lusk.underseerr.domain.model.AppError.NetworkError("Offline and no cached data"))
    }
    
    override suspend fun getUserStatistics(): Result<UserStatistics> = safeApiCall {
        val user = try { userKtorService.getCurrentUser() } catch (e: Exception) { null }
        
        // Use local database for statistics
        val allRequests = mediaRequestDao.getAllSync()

        val approved = allRequests.count { it.status == 2 }
        val declined = allRequests.count { it.status == 3 }
        val pending = allRequests.count { it.status == 1 }
        val available = allRequests.count { it.status == 4 || it.status == 5 }

        UserStatistics(
            totalRequests = if (user != null && user.requestCount > 0) user.requestCount else allRequests.size,
            approvedRequests = approved,
            declinedRequests = declined,
            pendingRequests = pending,
            availableRequests = available
        )
    }
}
