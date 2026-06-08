package org.freedomsuite.testing.integration

import org.freedomsuite.sync.SyncPreferences

class InMemorySyncPreferences : SyncPreferences {
    private val longs = mutableMapOf<String, Long>()
    private val bytes = mutableMapOf<String, ByteArray>()

    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getLong(key: String, default: Long): Long = longs[key] ?: default

    override fun putBytes(key: String, value: ByteArray) {
        bytes[key] = value
    }

    override fun getBytes(key: String): ByteArray? = bytes[key]
}
