package app.lusk.underseerr.domain.security

/**
 * Interface for managing Web Push encryption keys.
 */
interface WebPushKeyManager {
    /**
     * Gets existing or generates new Web Push keys.
     * Returns a pair of (p256dh, auth) base64 encoded strings.
     */
    suspend fun getOrCreateWebPushKeys(): Pair<String, String>
}
