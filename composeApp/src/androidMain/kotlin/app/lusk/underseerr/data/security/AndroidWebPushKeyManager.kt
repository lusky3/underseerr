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

    override suspend fun decrypt(payload: ByteArray, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        try {
            // 0. Handle accidental plaintext JSON (useful for manual testing via curl)
            val jsonString = String(payload, Charsets.UTF_8).trim()
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                android.util.Log.d("WebPushDecrypt", "Payload appears to be plaintext JSON, skipping decryption.")
                return@withContext jsonString
            }

            android.util.Log.d("WebPushDecrypt", "Attempting decryption. Payload size: ${payload.size}. Headers: $headers")

            val contentEncoding = headers["content-encoding"] ?: "aes128gcm"
            
            val (salt, rs, ciphertext, senderPubKeyRaw) = when (contentEncoding) {
                "aes128gcm" -> {
                    // RFC 8188 Header: salt(16) | rs(4) | idlen(1) | keyid(idlen)
                    if (payload.size < 21) throw Exception("Payload too small for aes128gcm header")
                    val s = payload.sliceArray(0 until 16)
                    val idlen = payload[20].toInt() and 0xFF
                    val pubKey = payload.sliceArray(21 until 21 + idlen)
                    val cipher = payload.sliceArray(21 + idlen until payload.size)
                    android.util.Log.d("WebPushDecrypt", "aes128gcm identified. idlen: $idlen")
                    Quadruple(s, 0, cipher, pubKey)
                }
                "aesgcm" -> {
                    // Legacy aesgcm uses headers for salt and cryptokey
                    val cryptoKeyHeader = (headers["crypto-key"] ?: headers["Crypto-Key"]) ?: throw Exception("Missing Crypto-Key header for aesgcm")
                    val encryptionHeader = (headers["encryption"] ?: headers["Encryption"]) ?: throw Exception("Missing Encryption header for aesgcm")
                    
                    val sHex = encryptionHeader.split("salt=").getOrNull(1)?.split(";")?.getOrNull(0) ?: throw Exception("Salt missing in header")
                    val s = Base64.getUrlDecoder().decode(sHex)
                    
                    val dhHex = cryptoKeyHeader.split("dh=").getOrNull(1)?.split(";")?.getOrNull(0) ?: throw Exception("DH missing in header")
                    val pubKey = Base64.getUrlDecoder().decode(dhHex)
                    
                    android.util.Log.d("WebPushDecrypt", "aesgcm identified. salt size: ${s.size}")
                    Quadruple(s, 0, payload, pubKey)
                }
                else -> throw Exception("Unsupported encoding: $contentEncoding")
            }

            val sharedSecret = deriveSharedSecret(senderPubKeyRaw)
            
            // 2. Perform HKDF
            // ikm = ECDH shared secret
            // info = "WebPush: info" || 0x00 || receiver_public || sender_public (for aesgcm)
            // or fixed labels for aes128gcm
            
            val (cek, nonce) = if (contentEncoding == "aes128gcm") {
                val authSecret = Base64.getUrlDecoder().decode(sharedPrefs.getString(KEY_AUTH, "")!!)
                val receiverPubKey = Base64.getUrlDecoder().decode(sharedPrefs.getString(KEY_P256DH, "")!!)
                
                // 1. PRK_key = HKDF-Extract(auth_secret, shared_secret)
                val prkKey = hkdfExtract(authSecret, sharedSecret)
                
                // 2. IKM = HKDF-Expand(PRK_key, "WebPush: info" || 0x00 || receiver_public || sender_public, 32)
                val info = "WebPush: info\u0000".toByteArray(Charsets.UTF_8) + receiverPubKey + senderPubKeyRaw
                val ikm = hkdfExpand(prkKey, info, 32)
                
                // 3. PRK = HKDF-Extract(salt, IKM)
                val prk = hkdfExtract(salt, ikm)
                
                // 4. Derive CEK and Nonce
                val derivedCek = hkdfExpand(prk, createInfo("aes128gcm", null, null), 16)
                val derivedNonce = hkdfExpand(prk, createInfo("nonce", null, null), 12)
                
                android.util.Log.d("WebPushDecrypt", "aes128gcm derivation complete")
                derivedCek to derivedNonce
            } else {
                // Legacy aesgcm (simplified for this task)
                val authSecret = Base64.getUrlDecoder().decode(sharedPrefs.getString(KEY_AUTH, "")!!)
                val prk = hkdfExtract(authSecret, sharedSecret)
                val context = createContext(senderPubKeyRaw)
                val derivedCek = hkdfExpand(prk, createInfo("aesgcm", context, null), 16)
                val derivedNonce = hkdfExpand(prk, createInfo("nonce", context, null), 12)
                
                android.util.Log.d("WebPushDecrypt", "aesgcm derivation complete")
                derivedCek to derivedNonce
            }

            // 3. Decrypt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, nonce)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cek, "AES"), spec)
            val decrypted = cipher.doFinal(ciphertext)

            // 4. Remove padding
            val result = if (contentEncoding == "aes128gcm") {
                 // For aes128gcm, padding is at the end, starting with 0x02, then zeros
                 var end = decrypted.size - 1
                 while (end >= 0 && decrypted[end] == 0.toByte()) end--
                 if (end >= 0 && decrypted[end] == 2.toByte()) {
                     decrypted.copyOfRange(0, end)
                 } else decrypted
            } else {
                 // For aesgcm, padding is at the beginning (2 bytes length)
                 val padLen = ((decrypted[0].toInt() and 0xFF) shl 8) or (decrypted[1].toInt() and 0xFF)
                 decrypted.copyOfRange(2 + padLen, decrypted.size)
            }

            val finalString = String(result, Charsets.UTF_8)
            android.util.Log.d("WebPushDecrypt", "Successfully decrypted: $finalString")
            finalString
        } catch (e: Exception) {
            android.util.Log.e("WebPushDecrypt", "Decryption error", e)
            throw e
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun deriveSharedSecret(senderPubKeyRaw: ByteArray): ByteArray {
        val privateKeyStr = sharedPrefs.getString(KEY_PRIVATE_KEY, null) ?: throw IllegalStateException("Private key not found for shared secret derivation")
        val privateKeyBytes = Base64.getUrlDecoder().decode(privateKeyStr)

        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val senderPublicKey = uncompressedToPublicKey(senderPubKeyRaw)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(senderPublicKey, true)
        return keyAgreement.generateSecret()
    }

    private fun createInfo(type: String, context: ByteArray?, label: String?): ByteArray {
        val typeBytes = when (type) {
            "aes128gcm" -> "Content-Encoding: aes128gcm\u0000".toByteArray(Charsets.UTF_8)
            "aesgcm" -> "Content-Encoding: aesgcm\u0000".toByteArray(Charsets.UTF_8)
            "nonce" -> "Content-Encoding: nonce\u0000".toByteArray(Charsets.UTF_8)
            "auth" -> "Content-Encoding: auth\u0000".toByteArray(Charsets.UTF_8)
            else -> type.toByteArray(Charsets.UTF_8)
        }
        return if (context != null) typeBytes + context else typeBytes
    }

    private fun createContext(senderPubKey: ByteArray): ByteArray {
        val receiverPubKey = Base64.getUrlDecoder().decode(sharedPrefs.getString(KEY_P256DH, "")!!)
        // Context = 0x00 || length_receiver || receiver || length_sender || sender
        return byteArrayOf(0) + 
               byteArrayOf(0, 65) + receiverPubKey + 
               byteArrayOf(0, 65) + senderPubKey
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
