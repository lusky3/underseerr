package app.lusk.underseerr.data.sync

import app.lusk.underseerr.data.network.ConnectivityStatus
import app.lusk.underseerr.data.network.NetworkManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Property-based tests for offline action queueing.
 * Feature: underseerr
 * Property 30: Offline Action Queueing
 * Validates: Requirements 7.5
 */
class OfflineActionQueueingPropertyTest : StringSpec({
    
    "Property 30.1: Actions queued while offline should be stored in queue" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100) { actionCount ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            
            // Act - Queue multiple actions while offline
            repeat(actionCount.coerceIn(1, 10)) {
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = { /* no-op */ }
                    )
                )
            }
            
            // Assert
            offlineQueue.getQueueSize() shouldBe actionCount.coerceIn(1, 10)
        }
    }
    
    "Property 30.2: Queued actions should be executed when connectivity is restored" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100, Arb.int(1..5)) { seed ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            var executionCount = 0
            
            // Act - Queue actions while offline
            repeat(seed) {
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = { executionCount++ }
                    )
                )
            }
            
            // Restore connectivity
            connectivityFlow.value = ConnectivityStatus.AVAILABLE
            delay(100) // Allow time for processing
            
            // Assert - All actions should be executed
            executionCount shouldBe seed
        }
    }
    
    "Property 30.3: Successfully executed actions should be removed from queue" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100, Arb.int(1..5)) { seed ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            
            // Act - Queue actions
            repeat(seed) {
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = { /* successful execution */ }
                    )
                )
            }
            
            val initialSize = offlineQueue.getQueueSize()
            initialSize shouldBe seed
            
            // Process queue
            offlineQueue.processQueue()
            delay(100)
            
            // Assert - Queue should be empty after successful execution
            offlineQueue.getQueueSize() shouldBe 0
        }
    }
    
    "Property 30.4: Failed actions should remain in queue for retry" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100, Arb.int(1..5)) { seed ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            
            // Act - Queue actions that will fail
            repeat(seed) {
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = { throw Exception("Network error") }
                    )
                )
            }
            
            // Process queue
            offlineQueue.processQueue()
            delay(100)
            
            // Assert - Failed actions should remain in queue
            offlineQueue.getQueueSize() shouldBe seed
        }
    }
    
    "Property 30.5: Queue should support different action types" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<List<ActionType>>(100, Arb.list(Arb.enum<ActionType>(), 1..10)) { actionTypes ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            val executedTypes = mutableListOf<ActionType>()
            
            // Act - Queue different action types
            actionTypes.forEach { type ->
                offlineQueue.queueAction(
                    QueuedAction(
                        type = type,
                        execute = { executedTypes.add(type) }
                    )
                )
            }
            
            // Process queue
            offlineQueue.processQueue()
            delay(100)
            
            // Assert - All action types should be executed
            executedTypes.size shouldBe actionTypes.size
            actionTypes.forEach { type ->
                executedTypes shouldContain type
            }
        }
    }
    
    "Property 30.6: Queue should preserve action order (FIFO)" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100, Arb.int(3..10)) { count ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            val executionOrder = mutableListOf<Int>()
            
            // Act - Queue actions with order tracking
            repeat(count) { index ->
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = { executionOrder.add(index) }
                    )
                )
            }
            
            // Process queue
            offlineQueue.processQueue()
            delay(100)
            
            // Assert - Actions should execute in FIFO order
            executionOrder shouldBe (0 until count).toList()
        }
    }
    
    "Property 30.7: Queue should handle mixed success and failure scenarios" {
        // Feature: underseerr, Property 30: Offline Action Queueing
        checkAll<Int>(100, Arb.int(4..10)) { count ->
            // Arrange
            val networkManager = mockk<NetworkManager>()
            val connectivityFlow = MutableStateFlow(ConnectivityStatus.UNAVAILABLE)
            every { networkManager.connectivityStatus } returns connectivityFlow
            
            val offlineQueue = OfflineQueue(networkManager)
            var successCount = 0
            
            // Act - Queue mix of successful and failing actions
            repeat(count) { index ->
                offlineQueue.queueAction(
                    QueuedAction(
                        type = ActionType.REQUEST_SUBMISSION,
                        execute = {
                            if (index % 2 == 0) {
                                successCount++
                            } else {
                                throw Exception("Simulated failure")
                            }
                        }
                    )
                )
            }
            
            val initialSize = offlineQueue.getQueueSize()
            
            // Process queue
            offlineQueue.processQueue()
            delay(100)
            
            // Assert - Only failed actions should remain
            val expectedFailures = count - (count / 2 + count % 2)
            offlineQueue.getQueueSize() shouldBe expectedFailures
            successCount shouldBe (count / 2 + count % 2)
        }
    }
})
