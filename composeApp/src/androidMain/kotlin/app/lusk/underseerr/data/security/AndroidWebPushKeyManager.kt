package app.lusk.underseerr.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.lusk.underseerr.domain.security.WebPushKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * Android implementation of WebPushKeyManager using Android Keystore and EncryptedSharedPreferences.
 */
class AndroidWebPushKeyManager(
    context: Context
) : WebPushKeyManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "web_push_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private companion object {
        const val KEY_P256DH = "p256dh"
        const val KEY_AUTH = "auth"
        const val KEY_PRIVATE_KEY = "private_key"
    }

    override suspend fun getOrCreateWebPushKeys(): Pair<String, String> = withContext(Dispatchers.IO) {
        val p256dh = sharedPrefs.getString(KEY_P256DH, null)
        val auth = sharedPrefs.getString(KEY_AUTH, null)
        val privateKey = sharedPrefs.getString(KEY_PRIVATE_KEY, null)

        if (p256dh != null && auth != null && privateKey != null) {
            return@withContext p256dh to auth
        }

        // Generate new keys
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()
        
        val publicKey = keyPair.public as ECPublicKey
        val x = publicKey.w.affineX.toByteArray().toUnsignedByteArray()
        val y = publicKey.w.affineY.toByteArray().toUnsignedByteArray()
        
        // Uncompressed format starts with 0x04, then X, then Y
        // Ensure X and Y are 32 bytes each
        val x32 = padTo32Bytes(x)
        val y32 = padTo32Bytes(y)
        val uncompressedKey = byteArrayOf(0x04) + x32 + y32
        
        val newP256dh = Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressedKey)

        // Save Private Key (PKCS#8)
        val newPrivateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyPair.private.encoded)

        // Generate auth secret (16 random bytes)
        val authBytes = ByteArray(16)
        SecureRandom().nextBytes(authBytes)
        val newAuth = Base64.getUrlEncoder().withoutPadding().encodeToString(authBytes)

        sharedPrefs.edit()
            .putString(KEY_P256DH, newP256dh)
            .putString(KEY_AUTH, newAuth)
            .putString(KEY_PRIVATE_KEY, newPrivateKey)
            .apply()

        newP256dh to newAuth
    }

    private fun padTo32Bytes(input: ByteArray): ByteArray {
        if (input.size == 32) return input
        if (input.size > 32) return input.takeLast(32).toByteArray()
        val output = ByteArray(32)
        System.arraycopy(input, 0, output, 32 - input.size, input.size)
        return output
    }

    private fun ByteArray.toUnsignedByteArray(): ByteArray {
        return if (this[0] == 0.toByte()) {
            this.copyOfRange(1, this.size)
        } else {
            this
        }
    }
}
