package org.freedomsuite.core.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeyDerivation {
    fun pbkdf2Sha256(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = 120_000,
        keyBits: Int = 256,
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, iterations, keyBits)
        return factory.generateSecret(spec).encoded
    }
}
