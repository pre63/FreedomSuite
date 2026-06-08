package org.freedomsuite.core.totp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {
    fun generate(
        secret: ByteArray,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
        digits: Int = 6,
        periodSeconds: Int = 30,
        timestampMs: Long = System.currentTimeMillis(),
    ): String {
        val counter = timestampMs / 1000 / periodSeconds
        val hash = hmac(secret, counter, algorithm)
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 10.0.pow(digits).toInt()
        return otp.toString().padStart(digits, '0')
    }

    fun secondsRemaining(periodSeconds: Int = 30, timestampMs: Long = System.currentTimeMillis()): Int {
        val elapsed = (timestampMs / 1000) % periodSeconds
        return periodSeconds - elapsed.toInt()
    }

    private fun hmac(secret: ByteArray, counter: Long, algorithm: TotpAlgorithm): ByteArray {
        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val buffer = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            buffer[i] = (value and 0xFF).toByte()
            value = value ushr 8
        }
        return mac.doFinal(buffer)
    }
}
