package org.freedomsuite.keyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey val word: String,
    val useCount: Long,
    val lastUsedAt: Long,
)

@Entity(tableName = "learned_bigrams", primaryKeys = ["previousWord", "nextWord"])
data class LearnedBigramEntity(
    val previousWord: String,
    val nextWord: String,
    val useCount: Long,
)
