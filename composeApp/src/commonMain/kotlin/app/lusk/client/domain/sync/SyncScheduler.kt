package app.lusk.client.domain.sync

/**
 * Interface for scheduling background synchronization.
 */
interface SyncScheduler {
    /**
     * Schedule a synchronization of offline requests.
     */
    fun scheduleOfflineSync()
}
