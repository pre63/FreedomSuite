package org.freedomsuite.messages.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAtEpochMs: Long,
    val memberCount: Int = 1,
)
