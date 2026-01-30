package app.lusk.underseerr.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.lusk.underseerr.domain.security.WebPushKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
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

    override suspend fun decrypt(payload: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val privateKeyStr = sharedPrefs.getString(KEY_PRIVATE_KEY, null) ?: throw IllegalStateException("Keys not initialized")
        val authSecretStr = sharedPrefs.getString(KEY_AUTH, null) ?: throw IllegalStateException("Keys not initialized")

        val decoder = Base64.getUrlDecoder()
        val privateKeyBytes = decoder.decode(privateKeyStr)
        val authSecret = decoder.decode(authSecretStr)

        // Parse aes128gcm Header: salt(16) | rs(4) | idlen(1) | keyid(idlen)
        if (payload.size < 21) throw IllegalArgumentException("Payload too short")
        val salt = payload.sliceArray(0 until 16)
        val idlen = payload[20].toInt() and 0xFF
        if (payload.size < 21 + idlen) throw IllegalArgumentException("Payload truncated")
        val senderPubKey = payload.sliceArray(21 until 21 + idlen)
        val ciphertext = payload.sliceArray(21 + idlen until payload.size)

        // 1. Shared Secret
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val senderPublicKey = uncompressedToPublicKey(senderPubKey)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(senderPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // 2. HKDF Multi-stage
        // PRK_IKM = HKDF-Extract(auth_secret, shared_secret)
        val prkIkm = hkdfExtract(authSecret, sharedSecret)
        
        // IKM = HKDF-Expand(PRK_IKM, "WebPush: info\0", 32)
        val ikm = hkdfExpand(prkIkm, "WebPush: info\u0000".toByteArray(), 32)
        
        // PRK = HKDF-Extract(salt, IKM)
        val prk = hkdfExtract(salt, ikm)
        
        // CEK & Nonce
        val cek = hkdfExpand(prk, "Content-Encoding: aes128gcm\u0000".toByteArray(), 16)
        val nonce = hkdfExpand(prk, "Content-Encoding: nonce\u0000".toByteArray(), 12)

        // 3. AES-GCM Decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cek, "AES"), spec)
        
        val decrypted = cipher.doFinal(ciphertext)
        
        // Remove padding (delimited by 0x02, followed by nulls)
        var end = decrypted.size - 1
        while (end >= 0 && decrypted[end] == 0.toByte()) {
            end--
        }
        if (end >= 0 && decrypted[end] == 2.toByte()) {
            decrypted.copyOfRange(0, end)
        } else {
            decrypted
        }
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(salt, "HmacSHA256"))
        return hmac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(prk, "HmacSHA256"))
        val result = ByteArray(length)
        var offset = 0
        var counter = 1
        var t = ByteArray(0)
        while (offset < length) {
            hmac.update(t)
            hmac.update(info)
            hmac.update(counter.toByte())
            t = hmac.doFinal()
            val chunk = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, chunk)
            offset += chunk
            counter++
        }
        return result
    }

    private fun uncompressedToPublicKey(data: ByteArray): PublicKey {
        val x = data.sliceArray(1..32)
        val y = data.sliceArray(33..64)
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec("secp256r1"))
        val ecSpec = params.getParameterSpec(ECParameterSpec::class.java)
        val point = ECPoint(BigInteger(1, x), BigInteger(1, y))
        val keySpec = ECPublicKeySpec(point, ecSpec)
        return KeyFactory.getInstance("EC").generatePublic(keySpec)
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
