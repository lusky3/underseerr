package app.lusk.client.data.repository

import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.UserApiService
import app.lusk.client.data.remote.model.ApiPermissions
import app.lusk.client.data.remote.model.ApiQuotaInfo
import app.lusk.client.data.remote.model.ApiRequestQuota
import app.lusk.client.data.remote.model.ApiUserProfile
import app.lusk.client.data.remote.model.ApiUserStatistics
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Property-based tests for profile information completeness.
 * Feature: overseerr-android-client, Property 19: Profile Information Completeness
 * Validates: Requirements 5.1
 */
class ProfileInformationPropertyTest : StringSpec({
    
    "Property 19.1: User profile contains all required fields" {
        checkAll<Triple<Int, String, String>>(100, 
            Arb.int(1..10000) to Arb.string(5..50) to Arb.string(5..30)
        ) { (userId, email, displayName) ->
            // Given
            val userApiService = mockk<UserApiService>()
            val safeApiCall = SafeApiCall()
            
            val apiProfile = ApiUserProfile(
                id = userId,
                email = email,
                displayName = displayName,
                avatar = "/avatar.jpg",
                requestCount = 10,
                permissions = ApiPermissions(
                    canRequest = true,
                    canManageRequests = false,
                    canManageUsers = false
                )
            )
            
            coEvery { userApiService.getCurrentUser() } returns apiProfile
            
            val repository = ProfileRepositoryImpl(userApiService, safeApiCall)
            
            // When
            val result = repository.getUserProfile()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<*>>()
            val profile = (result as Result.Success).data
            profile.id shouldBe userId
            profile.email shouldBe email
            profile.displayName shouldBe displayName
            profile.avatar shouldNotBe null
            profile.requestCount shouldBe 10
        }
    }
    
    "Property 19.2: User quota contains movie and TV limits" {
        checkAll<Pair<Int, Int>>(100, Arb.int(0..100) to Arb.int(0..100)) { (movieLimit, tvLimit) ->
            // Given
            val userApiService = mockk<UserApiService>()
            val safeApiCall = SafeApiCall()
            
            val apiQuota = ApiRequestQuota(
                movie = ApiQuotaInfo(
                    limit = movieLimit,
                    remaining = movieLimit / 2,
                    days = 7
                ),
                tv = ApiQuotaInfo(
                    limit = tvLimit,
                    remaining = tvLimit / 2,
                    days = 7
                )
            )
            
            coEvery { userApiService.getUserQuota() } returns apiQuota
            
            val repository = ProfileRepositoryImpl(userApiService, safeApiCall)
            
            // When
            val result = repository.getUserQuota()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<*>>()
            val quota = (result as Result.Success).data
            quota.movieLimit shouldBe movieLimit
            quota.tvLimit shouldBe tvLimit
            quota.movieRemaining shouldBe movieLimit / 2
            quota.tvRemaining shouldBe tvLimit / 2
        }
    }
    
    "Property 19.3: User statistics contains all request counts" {
        checkAll<List<Int>>(100, Arb.int(0..100)) { total ->
            // Given
            val userApiService = mockk<UserApiService>()
            val safeApiCall = SafeApiCall()
            
            val approved = total / 4
            val declined = total / 4
            val pending = total / 4
            val available = total - approved - declined - pending
            
            val apiStats = ApiUserStatistics(
                totalRequests = total,
                approvedRequests = approved,
                declinedRequests = declined,
                pendingRequests = pending,
                availableRequests = available
            )
            
            coEvery { userApiService.getUserStatistics() } returns apiStats
            
            val repository = ProfileRepositoryImpl(userApiService, safeApiCall)
            
            // When
            val result = repository.getUserStatistics()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<*>>()
            val stats = (result as Result.Success).data
            stats.totalRequests shouldBe total
            stats.approvedRequests shouldBe approved
            stats.declinedRequests shouldBe declined
            stats.pendingRequests shouldBe pending
            stats.availableRequests shouldBe available
        }
    }
    
    "Property 19.4: Profile handles missing optional fields gracefully" {
        checkAll<Pair<Int, String>>(100, Arb.int(1..10000) to Arb.string(5..50)) { (userId, email) ->
            // Given
            val userApiService = mockk<UserApiService>()
            val safeApiCall = SafeApiCall()
            
            val apiProfile = ApiUserProfile(
                id = userId,
                email = email,
                displayName = null, // Missing display name
                avatar = null, // Missing avatar
                requestCount = null, // Missing request count
                permissions = ApiPermissions(
                    canRequest = true,
                    canManageRequests = false,
                    canManageUsers = false
                )
            )
            
            coEvery { userApiService.getCurrentUser() } returns apiProfile
            
            val repository = ProfileRepositoryImpl(userApiService, safeApiCall)
            
            // When
            val result = repository.getUserProfile()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<*>>()
            val profile = (result as Result.Success).data
            profile.id shouldBe userId
            profile.email shouldBe email
            profile.displayName shouldBe email // Falls back to email
            profile.requestCount shouldBe 0 // Falls back to 0
        }
    }
    
    "Property 19.5: Quota handles unlimited quotas (null limits)" {
        checkAll<Int>(100) { seed ->
            // Given
            val userApiService = mockk<UserApiService>()
            val safeApiCall = SafeApiCall()
            
            val apiQuota = ApiRequestQuota(
                movie = ApiQuotaInfo(
                    limit = null, // Unlimited
                    remaining = null,
                    days = null
                ),
                tv = ApiQuotaInfo(
                    limit = null, // Unlimited
                    remaining = null,
                    days = null
                )
            )
            
            coEvery { userApiService.getUserQuota() } returns apiQuota
            
            val repository = ProfileRepositoryImpl(userApiService, safeApiCall)
            
            // When
            val result = repository.getUserQuota()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<*>>()
            val quota = (result as Result.Success).data
            quota.movieLimit shouldBe null
            quota.tvLimit shouldBe null
            quota.movieRemaining shouldBe null
            quota.tvRemaining shouldBe null
        }
    }
})
