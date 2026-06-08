package org.freedomsuite.core.applock

import android.content.Context
import org.freedomsuite.core.crypto.KeyDerivation
import org.freedomsuite.core.crypto.SecurePreferences
import java.security.MessageDigest
import java.security.SecureRandom

class AppLockSettings(context: Context) {
    private val prefs = SecurePreferences(context.applicationContext, "freedom_applock")

    val isPinConfigured: Boolean
        get() = prefs.getString(KEY_PIN_HASH) != null

    var gracePeriodSeconds: Int
        get() = prefs.getInt(KEY_GRACE_SECONDS, DEFAULT_GRACE_SECONDS)
        set(value) = prefs.putInt(KEY_GRACE_SECONDS, value.coerceIn(0, 300))

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.putBoolean(KEY_BIOMETRIC, value)

    fun savePin(pin: String) {
        require(pin.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.putString(KEY_PIN_SALT, salt.toHex())
        prefs.putInt(KEY_PIN_HASH_VERSION, HASH_VERSION_PBKDF2)
        prefs.putString(KEY_PIN_HASH, hashPinPbkdf2(pin, salt))
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_PIN_SALT) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH) ?: return false
        val salt = saltHex.hexToByteArray()
        return when (prefs.getInt(KEY_PIN_HASH_VERSION, HASH_VERSION_SHA256)) {
            HASH_VERSION_PBKDF2 -> storedHash == hashPinPbkdf2(pin, salt)
            else -> {
                val valid = storedHash == hashPinSha256(pin, salt)
                if (valid) upgradePinHash(pin, salt)
                valid
            }
        }
    }

    private fun upgradePinHash(pin: String, salt: ByteArray) {
        prefs.putInt(KEY_PIN_HASH_VERSION, HASH_VERSION_PBKDF2)
        prefs.putString(KEY_PIN_HASH, hashPinPbkdf2(pin, salt))
    }

    private fun hashPinPbkdf2(pin: String, salt: ByteArray): String =
        KeyDerivation.pbkdf2Sha256(pin.toCharArray(), salt, iterations = 100_000).toHex()

    private fun hashPinSha256(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        const val PIN_LENGTH = 4
        const val DEFAULT_GRACE_SECONDS = 20

        private const val HASH_VERSION_SHA256 = 1
        private const val HASH_VERSION_PBKDF2 = 2

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH_VERSION = "pin_hash_version"
        private const val KEY_GRACE_SECONDS = "grace_seconds"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
