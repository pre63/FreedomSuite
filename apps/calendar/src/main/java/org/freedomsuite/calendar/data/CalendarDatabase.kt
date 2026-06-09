package org.freedomsuite.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import org.freedomsuite.core.storage.EncryptedRoom

@Database(
    entities = [CalendarEntity::class, EventEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        private const val DB_NAME = "freedom_calendar.db"

        @Volatile
        private var instance: CalendarDatabase? = null

        fun getInstance(context: Context): CalendarDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): CalendarDatabase =
            EncryptedRoom.build(
                context = context,
                klass = CalendarDatabase::class.java,
                dbName = DB_NAME,
                prefsName = "freedom_calendar_keys",
                passphraseKey = "calendar_db_passphrase",
            )
    }
}
