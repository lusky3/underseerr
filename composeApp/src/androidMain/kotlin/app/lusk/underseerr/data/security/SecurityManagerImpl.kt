package app.lusk.underseerr.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.lusk.underseerr.domain.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Implementation of SecurityManager using Android Keystore and EncryptedSharedPreferences.
 */
class SecurityManagerImpl(
    private val context: Context
) : SecurityManager {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = createEncryptedPrefs()

    private companion object {
        const val TAG = "SecurityManagerImpl"
        const val PREFS_FILE_NAME = "secure_prefs"
        const val KEY_ALIAS = "underseerr_encryption_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
    }

    override suspend fun encryptData(data: String): String = withContext(Dispatchers.IO) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val combined = iv + encryptedBytes
        Base64.getEncoder().encodeToString(combined)
    }

    override suspend fun decryptData(encryptedData: String): String = withContext(Dispatchers.IO) {
        val secretKey = getOrCreateSecretKey()
        val combined = Base64.getDecoder().decode(encryptedData)
        
        // Extract IV and encrypted data
        val iv = combined.copyOfRange(0, 12)
        val encryptedBytes = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        String(decryptedBytes, Charsets.UTF_8)
    }

    override suspend fun storeSecureData(key: String, value: String) {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString(key, value).apply()
        }
    }

    override suspend fun retrieveSecureData(key: String): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(key, null)
    }

    override suspend fun clearSecureData() {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().clear().apply()
        }
    }


    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            createSecretKey()
        }
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Creates EncryptedSharedPreferences with graceful recovery from corrupted data.
     * If decryption fails (e.g., after app reinstall with different signing key),
     * the corrupted preferences file is deleted and recreated.
     */
    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Handle AEADBadTagException and other crypto errors
            // This can happen when the app is reinstalled with a different signing key
            Log.w(TAG, "Failed to decrypt secure preferences, resetting: ${e.message}")
            
            // Delete the corrupted preferences file
            val prefsFile = File(context.filesDir.parent, "shared_prefs/$PREFS_FILE_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
            
            // Recreate fresh encrypted preferences
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
