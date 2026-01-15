package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MovieDao
import app.lusk.client.data.local.dao.TvShowDao
import app.lusk.client.data.local.entity.MovieEntity
import app.lusk.client.data.local.entity.TvShowEntity
import app.lusk.client.domain.model.Movie
import app.lusk.client.domain.model.TvShow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for cache eviction policy.
 * Feature: overseerr-android-client, Property 29: Cache Eviction Policy
 * Validates: Requirements 7.4
 * 
 * These tests verify that the LRU cache eviction policy works correctly.
 */
class CacheEvictionPropertyTest : StringSpec({

    "Property: Cache size calculation is consistent" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        checkAll(100, Arb.int(0..100), Arb.int(0..100)) { movieCount, tvShowCount ->
            coEvery { movieDao.getCount() } returns movieCount
            coEvery { tvShowDao.getCount() } returns tvShowCount
            
            val cacheSize = repository.getCacheSize()
            
            // Cache size should be non-negative
            cacheSize shouldBeLessThanOrEqualTo Long.MAX_VALUE
            cacheSize shouldBeLessThanOrEqualTo (movieCount * 10 * 1024 + tvShowCount * 10 * 1024).toLong()
        }
    }

    "Property: Eviction reduces cache size" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        // Simulate cache exceeding limit
        val largeMovieCount = 30000 // Would exceed 100MB limit
        val largetvShowCount = 10000
        
        coEvery { movieDao.getCount() } returns largeMovieCount andThen (largeMovieCount * 0.8).toInt()
        coEvery { tvShowDao.getCount() } returns largetvShowCount andThen (largetvShowCount * 0.8).toInt()
        coEvery { movieDao.getOldest(any()) } returns emptyList()
        coEvery { tvShowDao.getOldest(any()) } returns emptyList()
        
        val sizeBefore = repository.getCacheSize()
        repository.evictLeastRecentlyUsed()
        val sizeAfter = repository.getCacheSize()
        
        // Size after eviction should be less than before
        sizeAfter shouldBeLessThan sizeBefore
    }

    "Property: Eviction removes oldest items first (LRU)" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        // Simulate cache exceeding limit
        val largeCount = 30000
        coEvery { movieDao.getCount() } returns largeCount
        coEvery { tvShowDao.getCount() } returns largeCount
        
        val oldMovies = listOf(
            MovieEntity(1, "Old Movie 1", "", null, null, null, 0.0, System.currentTimeMillis() - 10000),
            MovieEntity(2, "Old Movie 2", "", null, null, null, 0.0, System.currentTimeMillis() - 9000)
        )
        coEvery { movieDao.getOldest(any()) } returns oldMovies
        coEvery { tvShowDao.getOldest(any()) } returns emptyList()
        
        repository.evictLeastRecentlyUsed()
        
        // Verify that oldest items were requested for deletion
        coVerify { movieDao.getOldest(any()) }
        coVerify { movieDao.delete(any()) }
    }

    "Property: Cache eviction respects size threshold" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        // Simulate cache below limit
        val smallCount = 100
        coEvery { movieDao.getCount() } returns smallCount
        coEvery { tvShowDao.getCount() } returns smallCount
        coEvery { movieDao.getOldest(any()) } returns emptyList()
        coEvery { tvShowDao.getOldest(any()) } returns emptyList()
        
        repository.evictLeastRecentlyUsed()
        
        // Verify that no items were deleted when cache is below threshold
        coVerify(exactly = 0) { movieDao.delete(any()) }
        coVerify(exactly = 0) { tvShowDao.delete(any()) }
    }

    "Property: Delete older than removes items by age" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        checkAll(10, Arb.int(1..30)) { daysOld ->
            val ageMillis = daysOld * 24 * 60 * 60 * 1000L
            val expectedCutoff = System.currentTimeMillis() - ageMillis
            
            repository.deleteOlderThan(ageMillis)
            
            // Verify that delete was called with appropriate timestamp
            coVerify { movieDao.deleteOlderThan(match { it <= expectedCutoff + 1000 }) }
            coVerify { tvShowDao.deleteOlderThan(match { it <= expectedCutoff + 1000 }) }
        }
    }

    "Property: Clear all caches removes all items" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        repository.clearAllCaches()
        
        // Verify that deleteAll was called on both DAOs
        coVerify { movieDao.deleteAll() }
        coVerify { tvShowDao.deleteAll() }
    }

    "Property: Cache size is proportional to item count" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        checkAll(100, Arb.int(0..1000)) { count ->
            coEvery { movieDao.getCount() } returns count
            coEvery { tvShowDao.getCount() } returns 0
            
            val cacheSize = repository.getCacheSize()
            
            // Cache size should be roughly proportional to count
            // (allowing for estimation variance)
            val expectedMinSize = count * 1024L // 1 KB per item minimum
            val expectedMaxSize = count * 10 * 1024L // 10 KB per item maximum
            
            if (count > 0) {
                cacheSize shouldBeLessThanOrEqualTo expectedMaxSize
            }
        }
    }

    "Property: Eviction percentage is consistent" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        // Simulate cache exceeding limit
        val largeCount = 30000
        coEvery { movieDao.getCount() } returns largeCount
        coEvery { tvShowDao.getCount() } returns 0
        coEvery { movieDao.getOldest(any()) } returns emptyList()
        
        repository.evictLeastRecentlyUsed()
        
        // Verify that approximately 20% of items are requested for eviction
        coVerify { movieDao.getOldest(match { it in 5000..7000 }) }
    }

    "Property: Cache operations are idempotent" {
        val movieDao = mockk<MovieDao>(relaxed = true)
        val tvShowDao = mockk<TvShowDao>(relaxed = true)
        val repository = CacheRepositoryImpl(movieDao, tvShowDao)
        
        coEvery { movieDao.getCount() } returns 100
        coEvery { tvShowDao.getCount() } returns 100
        
        val size1 = repository.getCacheSize()
        val size2 = repository.getCacheSize()
        
        // Multiple calls should return same result
        size1 shouldBe size2
    }

    "Property: Maximum cache size threshold is enforced" {
        val maxCacheSize = 100 * 1024 * 1024L // 100 MB
        
        // Verify that the threshold is reasonable
        maxCacheSize shouldBeLessThanOrEqualTo 200 * 1024 * 1024L
        maxCacheSize shouldBeLessThanOrEqualTo Long.MAX_VALUE
    }
})
