package org.freedomsuite.core.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

object EncryptedRoom {
    fun <T : RoomDatabase> build(
        context: Context,
        klass: Class<T>,
        dbName: String,
        prefsName: String,
        passphraseKey: String,
    ): T {
        EncryptedDatabaseFactory.loadLibs(context)
        val passphrase = DatabaseKeyManager(context, prefsName, passphraseKey).getOrCreatePassphrase()
        return Room.databaseBuilder(context, klass, dbName)
            .openHelperFactory(EncryptedDatabaseFactory.create(passphrase))
            .fallbackToDestructiveMigration()
            .build()
    }
}
