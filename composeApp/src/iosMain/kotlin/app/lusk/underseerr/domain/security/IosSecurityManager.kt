package app.lusk.underseerr.domain.security

import platform.Foundation.NSUserDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS Implementation of SecurityManager using NSUserDefaults for persistence.
 * Note: For production, this should be migrated to Keychain for better security.
 */
class IosSecurityManager : SecurityManager {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val keyPrefix = "secure_storage_"

    override suspend fun encryptData(data: String): String {
        // TODO: Implement proper encryption if needed
        return data
    }

    override suspend fun decryptData(encryptedData: String): String {
        // TODO: Implement proper decryption if needed
        return encryptedData
    }

    override suspend fun storeSecureData(key: String, value: String) {
        val fullKey = keyPrefix + key
        println("IosSecurityManager: Storing data for key '$fullKey' (value length: ${value.length})")
        defaults.setObject(value, forKey = fullKey)
    }

    override suspend fun retrieveSecureData(key: String): String? {
        val fullKey = keyPrefix + key
        val value = defaults.stringForKey(fullKey)
        println("IosSecurityManager: Retrieved data for key '$fullKey': ${if (value != null) "FOUND (length ${value.length})" else "NULL"}")
        return value
    }

    override suspend fun clearSecureData() {
        val dictionary = defaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            if (key is String && key.startsWith(keyPrefix)) {
                defaults.removeObjectForKey(key)
            }
        }
    }
}
