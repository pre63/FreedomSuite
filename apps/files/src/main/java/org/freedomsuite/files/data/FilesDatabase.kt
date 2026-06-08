package org.freedomsuite.files.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.storage.DatabaseKeyManager
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [FolderEntity::class, FileItemEntity::class, ImageAnalysisEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FilesDatabase : RoomDatabase() {
    abstract fun filesDao(): FilesDao

    companion object {
        private const val DB_NAME = "freedom_files.db"

        @Volatile
        private var instance: FilesDatabase? = null

        fun getInstance(context: Context): FilesDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): FilesDatabase =
            EncryptedRoom.build(
                context = context,
                klass = FilesDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_files_keys",
                passphraseKey = "files_db_passphrase",
            )
    }
}

class FilesKeyManager(context: Context) {
    private val fileKeys = DatabaseKeyManager(context, "freedom_files_keys", KEY_FILE_DEK)

    fun getOrCreateFileKey(): ByteArray = fileKeys.getOrCreatePassphrase()

    companion object {
        private const val KEY_FILE_DEK = "files_content_dek"
    }
}
