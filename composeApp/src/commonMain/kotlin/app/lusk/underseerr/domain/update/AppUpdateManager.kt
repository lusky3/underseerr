package app.lusk.underseerr.domain.update

/**
 * Interface for checking and managing app updates.
 * 
 * Android: Uses Google Play In-App Updates API.
 * iOS: Placeholder for App Store update checks.
 */
interface AppUpdateManager {

    /**
     * Checks whether an update is available on the app store.
     * @return true if a newer version is available
     */
    suspend fun isUpdateAvailable(): Boolean

    /**
     * Starts the update flow (navigates to the app store or starts an in-app update).
     */
    suspend fun startUpdate()
}
