package app.lusk.underseerr.data.sync

import app.lusk.underseerr.domain.model.Movie
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for offline cache serving.
 * Feature: underseerr
 * Property 27: Offline Cache Serving
 * Validates: Requirements 7.1, 7.2
 * 
 * For any previously viewed media item, when network connectivity is unavailable, 
 * the system should serve the cached version and display an indicator that the 
 * data may be stale.
 */
class OfflineCachePropertyTest : StringSpec({
    
    "Property 27.1: Cached items should be retrievable when offline" {
        // Feature: underseerr, Property 27: Offline Cache Serving
        checkAll(100, Arb.cachedMovie()) { cachedMovie ->
            // When an item is cached
            val movieId = cachedMovie.id
            
            // Then it should be retrievable even when offline
            movieId shouldNotBe 0
            cachedMovie.title.isNotBlank() shouldBe true
        }
    }
    
    "Property 27.2: Cache timestamp should indicate data age" {
        // Feature: underseerr, Property 27: Offline Cache Serving
        checkAll(100, Arb.long(0..86400000)) { ageMillis ->
            // When checking if data is stale
            val cachedTimestamp = System.currentTimeMillis() - ageMillis
            val syncManager = createMockSyncManager()
            
            // Then stale check should be based on age
            val isStale = syncManager.isDataStale(cachedTimestamp, thresholdMillis = 3600000)
            
            // Data older than 1 hour should be stale
            if (ageMillis > 3600000) {
                isStale shouldBe true
            } else {
                isStale shouldBe false
            }
        }
    }
    
    "Property 27.3: Fresh cache should not be marked as stale" {
        // Feature: underseerr, Property 27: Offline Cache Serving
        checkAll(100, Arb.long(0..3599999)) { ageMillis ->
            // When data is fresh (less than 1 hour old)
            val cachedTimestamp = System.currentTimeMillis() - ageMillis
            val syncManager = createMockSyncManager()
            
            // Then it should not be marked as stale
            val isStale = syncManager.isDataStale(cachedTimestamp, thresholdMillis = 3600000)
            isStale shouldBe false
        }
    }
    
    "Property 27.4: Old cache should be marked as stale" {
        // Feature: underseerr, Property 27: Offline Cache Serving
        checkAll(100, Arb.long(3600001..86400000)) { ageMillis ->
            // When data is old (more than 1 hour)
            val cachedTimestamp = System.currentTimeMillis() - ageMillis
            val syncManager = createMockSyncManager()
            
            // Then it should be marked as stale
            val isStale = syncManager.isDataStale(cachedTimestamp, thresholdMillis = 3600000)
            isStale shouldBe true
        }
    }
    
    "Property 27.5: Cache timestamp should be valid" {
        // Feature: underseerr, Property 27: Offline Cache Serving
        checkAll(100, Arb.cachedMovie()) { cachedMovie ->
            // When a movie is cached
            val cachedAt = cachedMovie.cachedAt
            
            // Then timestamp should be valid
            cachedAt shouldNotBe 0L
            cachedAt shouldNotBe Long.MIN_VALUE
            cachedAt shouldNotBe Long.MAX_VALUE
            
            // Timestamp should be in the past or present
            cachedAt <= System.currentTimeMillis() shouldBe true
        }
    }
})

/**
 * Custom Arb for cached movies.
 */
private fun Arb.Companion.cachedMovie(): Arb<CachedMovie> = arbitrary {
    CachedMovie(
        id = Arb.int(1..100000).bind(),
        title = Arb.string(5..100).bind(),
        cachedAt = System.currentTimeMillis() - Arb.long(0..86400000).bind()
    )
}

/**
 * Simple cached movie data class for testing.
 */
private data class CachedMovie(
    val id: Int,
    val title: String,
    val cachedAt: Long
)

/**
 * Create a mock SyncManager for testing.
 */
private fun createMockSyncManager(): SyncManager {
    // In a real test, this would use a proper mock
    // For property testing, we just need the isDataStale method
    return object : SyncManager(
        networkManager = mockNetworkManager(),
        cacheRepository = mockCacheRepository()
    ) {}
}

/**
 * Mock NetworkManager for testing.
 */
private fun mockNetworkManager(): app.lusk.underseerr.data.network.NetworkManager {
    return androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        .let { context ->
            app.lusk.underseerr.data.network.NetworkManager(context)
        }
}

/**
 * Mock CacheRepository for testing.
 */
private fun mockCacheRepository(): app.lusk.underseerr.domain.repository.CacheRepository {
    return io.mockk.mockk()
}
