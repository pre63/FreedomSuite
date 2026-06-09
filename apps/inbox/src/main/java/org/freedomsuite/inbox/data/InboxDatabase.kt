package org.freedomsuite.inbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [MailMessageEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class InboxDatabase : RoomDatabase() {
    abstract fun inboxDao(): InboxDao

    companion object {
        private const val DB_NAME = "freedom_inbox.db"

        @Volatile
        private var instance: InboxDatabase? = null

        fun getInstance(context: Context): InboxDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): InboxDatabase =
            EncryptedRoom.build(
                context = context,
                klass = InboxDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_inbox_keys",
                passphraseKey = "inbox_db_passphrase",
            )
    }
}
