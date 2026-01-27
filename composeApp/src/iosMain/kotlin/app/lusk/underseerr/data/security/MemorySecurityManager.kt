package app.lusk.underseerr.data.security

import app.lusk.underseerr.domain.security.SecurityManager

/**
 * Simple in-memory security manager for iOS (Placeholder).
 * In a real app, this should use iOS Keychain.
 */
class MemorySecurityManager : SecurityManager {
    private val storage = mutableMapOf<String, String>()

    override suspend fun encryptData(data: String): String {
        // TODO: Implement actual encryption
        return "encrypted_$data" 
    }

    override suspend fun decryptData(encryptedData: String): String {
        // TODO: Implement actual decryption
        return encryptedData.removePrefix("encrypted_")
    }

    override suspend fun storeSecureData(key: String, value: String) {
        storage[key] = value
    }

    override suspend fun retrieveSecureData(key: String): String? {
        return storage[key]
    }

    override suspend fun clearSecureData() {
        storage.clear()
    }
}
