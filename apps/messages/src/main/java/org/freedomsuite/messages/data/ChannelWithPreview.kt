package org.freedomsuite.messages.data

import androidx.room.Embedded
import androidx.room.Relation

data class ChannelWithPreview(
    @Embedded val channel: ChannelEntity,
    val latestPreview: String?,
)
