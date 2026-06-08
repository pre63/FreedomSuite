package org.freedomsuite.sync

import android.content.Context
import org.freedomsuite.core.crypto.SecurePreferences

interface SyncPreferences {
    fun putLong(key: String, value: Long)
    fun getLong(key: String, default: Long): Long
    fun putBytes(key: String, value: ByteArray)
    fun getBytes(key: String): ByteArray?
}

internal class AndroidSyncPreferences(context: Context, name: String) : SyncPreferences {
    private val prefs = SecurePreferences(context.applicationContext, name)

    override fun putLong(key: String, value: Long) = prefs.putLong(key, value)
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putBytes(key: String, value: ByteArray) = prefs.putBytes(key, value)
    override fun getBytes(key: String): ByteArray? = prefs.getBytes(key)
}
