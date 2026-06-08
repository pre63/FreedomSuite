package org.freedomsuite.messages.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("channelId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val body: String,
    val createdAtEpochMs: Long,
    val authorLabel: String = "You",
    val attachmentPath: String? = null,
    val attachmentMimeType: String? = null,
)
