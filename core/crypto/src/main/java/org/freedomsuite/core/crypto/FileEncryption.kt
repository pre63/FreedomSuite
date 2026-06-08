package org.freedomsuite.core.crypto

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object FileEncryption {
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return byteArrayOf(CryptoAlgorithms.VERSION.toByte()) + iv + ciphertext
    }

    fun decrypt(key: ByteArray, payload: ByteArray): ByteArray {
        require(payload.size > IV_BYTES) { "payload too short" }
        val (iv, ciphertext) = if (payload[0].toInt() == CryptoAlgorithms.VERSION) {
            require(payload.size > IV_BYTES + 1) { "payload too short" }
            payload.copyOfRange(1, 1 + IV_BYTES) to payload.copyOfRange(1 + IV_BYTES, payload.size)
        } else {
            // Legacy blobs written before the format version byte was added.
            payload.copyOfRange(0, IV_BYTES) to payload.copyOfRange(IV_BYTES, payload.size)
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun writeEncrypted(key: ByteArray, file: File, plaintext: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(encrypt(key, plaintext))
    }

    fun readEncrypted(key: ByteArray, file: File): ByteArray {
        return decrypt(key, file.readBytes())
    }
}
