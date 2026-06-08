package org.freedomsuite.messages.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.crypto.SecurePreferences
import org.freedomsuite.core.storage.DatabaseKeyManager
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [ChannelEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MessagesDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao

    companion object {
        private const val DB_NAME = "freedom_messages.db"

        @Volatile
        private var instance: MessagesDatabase? = null

        fun getInstance(context: Context): MessagesDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): MessagesDatabase =
            EncryptedRoom.build(
                context = context,
                klass = MessagesDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_messages_keys",
                passphraseKey = "messages_db_passphrase",
            )
    }
}

class MessagesKeyManager(context: Context) {
    private val dbKeys = DatabaseKeyManager(context, "freedom_messages_keys", KEY_DB_PASSPHRASE)
    private val attachmentKeys = DatabaseKeyManager(context, "freedom_messages_keys", KEY_ATTACHMENT_DEK)

    fun getOrCreateDbPassphrase(): ByteArray = dbKeys.getOrCreatePassphrase()

    fun getOrCreateAttachmentKey(): ByteArray = attachmentKeys.getOrCreatePassphrase()

    companion object {
        private const val KEY_DB_PASSPHRASE = "messages_db_passphrase"
        private const val KEY_ATTACHMENT_DEK = "messages_attachment_dek"
    }
}
