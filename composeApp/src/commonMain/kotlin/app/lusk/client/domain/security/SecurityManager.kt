package app.lusk.client.domain.security

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
     * Clears all securely stored data.
     */
    suspend fun clearSecureData()
}
