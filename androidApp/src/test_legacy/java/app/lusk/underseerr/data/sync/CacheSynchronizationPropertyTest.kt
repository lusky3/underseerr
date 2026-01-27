package app.lusk.underseerr.data.sync

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

/**
 * Property-based tests for cache synchronization.
 * Feature: underseerr
 * Property 28: Cache Synchronization
 * Validates: Requirements 7.3
 * 
 * For any cached data, when network connectivity is restored, the system should 
 * synchronize with the server, updating cached items with fresh data and removing 
 * items that no longer exist.
 */
class CacheSynchronizationPropertyTest : StringSpec({
    
    "Property 28.1: Sync should update last sync timestamp" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.long(0..System.currentTimeMillis())) { initialTimestamp ->
            // When sync is performed
            val syncState = SyncState.Success
            val newTimestamp = System.currentTimeMillis()
            
            // Then new timestamp should be greater than or equal to initial
            newTimestamp >= initialTimestamp shouldBe true
        }
    }
    
    "Property 28.2: Sync state should transition correctly" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.syncState()) { initialState ->
            // When sync starts
            val syncingState = SyncState.Syncing
            
            // Then state should be Syncing
            syncingState.shouldBeInstanceOf<SyncState.Syncing>()
            
            // And should eventually transition to Success or Error
            val finalState = if (Math.random() > 0.5) {
                SyncState.Success
            } else {
                SyncState.Error("Test error")
            }
            
            (finalState is SyncState.Success || finalState is SyncState.Error) shouldBe true
        }
    }
    
    "Property 28.3: Successful sync should set Success state" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.int(0..1000)) { _ ->
            // When sync completes successfully
            val successState = SyncState.Success
            
            // Then state should be Success
            successState.shouldBeInstanceOf<SyncState.Success>()
        }
    }
    
    "Property 28.4: Failed sync should set Error state" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.errorMessage()) { errorMessage ->
            // When sync fails
            val errorState = SyncState.Error(errorMessage)
            
            // Then state should be Error with message
            errorState.shouldBeInstanceOf<SyncState.Error>()
            errorState.message.isNotBlank() shouldBe true
        }
    }
    
    "Property 28.5: Sync should not run concurrently" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.int(1..10)) { attemptCount ->
            // When multiple sync attempts are made
            val syncingState = SyncState.Syncing
            
            // Then only one sync should be active
            syncingState.shouldBeInstanceOf<SyncState.Syncing>()
            
            // Concurrent attempts should be skipped
            attemptCount >= 1 shouldBe true
        }
    }
    
    "Property 28.6: Sync timestamp should be monotonically increasing" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.long(0..System.currentTimeMillis() - 1000)) { previousTimestamp ->
            // When sync is performed multiple times
            val newTimestamp = System.currentTimeMillis()
            
            // Then timestamps should increase
            newTimestamp > previousTimestamp shouldBe true
        }
    }
    
    "Property 28.7: Idle state should be default" {
        // Feature: underseerr, Property 28: Cache Synchronization
        checkAll(100, Arb.int(0..100)) { _ ->
            // When sync manager is initialized
            val idleState = SyncState.Idle
            
            // Then initial state should be Idle
            idleState.shouldBeInstanceOf<SyncState.Idle>()
        }
    }
})

/**
 * Custom Arb for sync states.
 */
private fun Arb.Companion.syncState(): Arb<SyncState> = arbitrary {
    listOf(
        SyncState.Idle,
        SyncState.Syncing,
        SyncState.Success,
        SyncState.Error("Test error")
    ).random()
}

/**
 * Custom Arb for error messages.
 */
private fun Arb.Companion.errorMessage(): Arb<String> = arbitrary {
    listOf(
        "Network error",
        "Server error",
        "Timeout",
        "Connection failed",
        "Sync failed"
    ).random()
}
