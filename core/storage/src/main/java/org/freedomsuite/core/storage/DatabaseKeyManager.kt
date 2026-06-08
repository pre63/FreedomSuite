package org.freedomsuite.core.storage

import android.content.Context
import org.freedomsuite.core.crypto.SecurePreferences
import java.security.SecureRandom

/**
 * Generates and stores SQLCipher passphrases in Keystore-backed prefs.
 */
class DatabaseKeyManager(
    context: Context,
    prefsName: String,
    private val keyName: String,
) {
    private val prefs = SecurePreferences(context.applicationContext, prefsName)

    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getBytes(keyName)
        if (existing != null) return existing
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.putBytes(keyName, generated)
        return generated
    }
}
