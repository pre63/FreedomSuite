package org.freedomsuite.auth.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [AuthAccountEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AuthDatabase : RoomDatabase() {
    abstract fun authDao(): AuthDao

    companion object {
        private const val DB_NAME = "freedom_auth.db"

        @Volatile
        private var instance: AuthDatabase? = null

        fun getInstance(context: Context): AuthDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AuthDatabase =
            EncryptedRoom.build(
                context = context,
                klass = AuthDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_auth_keys",
                passphraseKey = "auth_db_passphrase",
            )
    }
}
