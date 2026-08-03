package app.lusk.underseerr.domain.security

/**
 * Interface for security operations including encryption and secure storage.
 */
interface SecurityManager {
    /**
     * Encrypts data.
     */
    suspend fun encryptData(data: String): String

    /**
     * Decrypts data.
     */
    suspend fun decryptData(encryptedData: String): String

    /**
     * Stores data securely.
     */
    suspend fun storeSecureData(key: String, value: String)

    /**
     * Retrieves securely stored data.
     */
    suspend fun retrieveSecureData(key: String): String?

    /**
     * Removes a single securely stored value.
     *
     * Needed to drop one dead credential (e.g. a revoked Plex token) without
     * tearing down the whole session.
     */
    suspend fun removeSecureData(key: String)

    /**
     * Clears all securely stored data.
     */
    suspend fun clearSecureData()
}
