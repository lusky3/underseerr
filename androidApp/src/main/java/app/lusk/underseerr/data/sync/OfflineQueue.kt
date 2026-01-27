package app.lusk.underseerr.data.sync

import app.lusk.underseerr.data.network.NetworkManager
import app.lusk.underseerr.data.network.NetworkManager.ConnectivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID


/**
 * Queue for actions that require network connectivity.
 * Feature: underseerr
 * Validates: Requirements 7.5
 * Property 30: Offline Action Queueing
 */
class OfflineQueue(
    private val networkManager: NetworkManager
) {
    
    private val queueScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    
    private val _queuedActions = MutableStateFlow<List<QueuedAction>>(emptyList())
    val queuedActions: StateFlow<List<QueuedAction>> = _queuedActions.asStateFlow()
    
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()
    
    init {
        observeConnectivity()
    }
    
    /**
     * Observe connectivity and process queue when online.
     */
    private fun observeConnectivity() {
        queueScope.launch {
            networkManager.connectivityStatus.collect { status ->
                if (status == ConnectivityStatus.CONNECTED && _queuedActions.value.isNotEmpty()) {
                    processQueue()
                }
            }
        }
    }
    
    /**
     * Queue an action for later execution.
     * Property 30: Offline Action Queueing
     */
    suspend fun queueAction(action: QueuedAction) {
        mutex.withLock {
            _queuedActions.value = _queuedActions.value + action
        }
    }
    
    /**
     * Process all queued actions.
     */
    suspend fun processQueue() {
        if (_processingState.value is ProcessingState.Processing) {
            // Already processing
            return
        }
        
        _processingState.value = ProcessingState.Processing
        
        val actionsToProcess = _queuedActions.value.toList()
        val successfulActions = mutableListOf<String>()
        val failedActions = mutableListOf<Pair<String, String>>()
        
        for (action in actionsToProcess) {
            try {
                // Execute the action
                action.execute()
                successfulActions.add(action.id)
            } catch (e: Exception) {
                failedActions.add(action.id to (e.message ?: "Unknown error"))
            }
        }
        
        // Remove successful actions from queue
        mutex.withLock {
            _queuedActions.value = _queuedActions.value.filter { action ->
                action.id !in successfulActions
            }
        }
        
        _processingState.value = if (failedActions.isEmpty()) {
            ProcessingState.Success(successfulActions.size)
        } else {
            ProcessingState.PartialSuccess(
                successful = successfulActions.size,
                failed = failedActions.size
            )
        }
        
        // Reset to idle after a delay
        kotlinx.coroutines.delay(2000)
        _processingState.value = ProcessingState.Idle
    }
    
    /**
     * Clear all queued actions.
     */
    suspend fun clearQueue() {
        mutex.withLock {
            _queuedActions.value = emptyList()
        }
    }
    
    /**
     * Remove a specific action from the queue.
     */
    suspend fun removeAction(actionId: String) {
        mutex.withLock {
            _queuedActions.value = _queuedActions.value.filter { it.id != actionId }
        }
    }
    
    /**
     * Get the number of queued actions.
     */
    fun getQueueSize(): Int = _queuedActions.value.size
}

/**
 * Represents an action that can be queued for later execution.
 */
data class QueuedAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val timestamp: Long = System.currentTimeMillis(),
    val execute: suspend () -> Unit
)

/**
 * Types of actions that can be queued.
 */
enum class ActionType {
    REQUEST_SUBMISSION,
    REQUEST_CANCELLATION,
    PROFILE_UPDATE,
    SETTINGS_UPDATE,
    OTHER
}

/**
 * Sealed class representing processing state.
 */
sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Processing : ProcessingState()
    data class Success(val processedCount: Int) : ProcessingState()
    data class PartialSuccess(val successful: Int, val failed: Int) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
