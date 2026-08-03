package app.lusk.underseerr.domain.security

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [SecurityManager], backed by NSUserDefaults.
 *
 * SECURITY WARNING — this is NOT secure storage. NSUserDefaults is an unencrypted plist in
 * the app container: it is readable on a jailbroken or file-system-dumped device and is
 * included in unencrypted iTunes/Finder backups. The Plex token, Overseerr session cookie
 * and API key stored here are therefore effectively stored in the clear.
 *
 * This must be migrated to the Keychain (kSecClassGenericPassword with
 * kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly) before the iOS app ships. The migration
 * has to read through to NSUserDefaults for values written by this version, copy them into
 * the Keychain on first read, and only then delete the NSUserDefaults copy — otherwise
 * existing installs are silently signed out.
 */
class IosSecurityManager : SecurityManager {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val keyPrefix = "secure_storage_"

    // No-op pass-through: the values are stored as-is (see the class-level warning).
    // Returning the input unchanged keeps store/retrieve symmetric; do not "improve" one
    // side of this pair on its own or previously stored values become unreadable.
    override suspend fun encryptData(data: String): String = data

    override suspend fun decryptData(encryptedData: String): String = encryptedData

    override suspend fun storeSecureData(key: String, value: String) {
        defaults.setObject(value, forKey = keyPrefix + key)
    }

    override suspend fun retrieveSecureData(key: String): String? {
        return defaults.stringForKey(keyPrefix + key)
    }

    override suspend fun removeSecureData(key: String) {
        defaults.removeObjectForKey(keyPrefix + key)
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
