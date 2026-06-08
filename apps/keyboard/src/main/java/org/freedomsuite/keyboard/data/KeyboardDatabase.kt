package org.freedomsuite.keyboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [UserWordEntity::class, LearnedBigramEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KeyboardDatabase : RoomDatabase() {
    abstract fun keyboardDao(): KeyboardDao

    companion object {
        private const val DB_NAME = "freedom_keyboard.db"

        @Volatile
        private var instance: KeyboardDatabase? = null

        fun getInstance(context: Context): KeyboardDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): KeyboardDatabase =
            EncryptedRoom.build(
                context = context,
                klass = KeyboardDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_keyboard_keys",
                passphraseKey = "keyboard_db_passphrase",
            )
    }
}
