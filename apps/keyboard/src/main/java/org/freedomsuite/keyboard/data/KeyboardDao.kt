package org.freedomsuite.keyboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KeyboardDao {
    @Query("SELECT * FROM user_words ORDER BY useCount DESC LIMIT :limit")
    suspend fun topUserWords(limit: Int = 5000): List<UserWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserWord(entity: UserWordEntity)

    @Query("SELECT * FROM learned_bigrams ORDER BY useCount DESC LIMIT :limit")
    suspend fun topBigrams(limit: Int = 10000): List<LearnedBigramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBigram(entity: LearnedBigramEntity)

    @Query("SELECT COUNT(*) FROM user_words")
    suspend fun userWordCount(): Int
}
