package org.freedomsuite.core.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Creates a SQLCipher [SupportSQLiteOpenHelper.Factory] for Room databases.
 * Passphrase must come from [org.freedomsuite.core.crypto.SecurePreferences] or Keystore.
 */
object EncryptedDatabaseFactory {
    fun create(passphrase: ByteArray): SupportSQLiteOpenHelper.Factory {
        System.loadLibrary("sqlcipher")
        return SupportOpenHelperFactory(passphrase)
    }

    fun loadLibs(context: Context) {
        System.loadLibrary("sqlcipher")
    }
}
