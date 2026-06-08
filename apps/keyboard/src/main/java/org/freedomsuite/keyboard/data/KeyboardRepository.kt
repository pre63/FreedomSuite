package org.freedomsuite.keyboard.data

import android.content.Context
import org.freedomsuite.core.keyboard.FrequencyDictionary
import org.freedomsuite.core.keyboard.SuggestionEngine

class KeyboardRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = KeyboardDatabase.getInstance(appContext).keyboardDao()
    val dictionary = FrequencyDictionary(appContext)
    val suggestions = SuggestionEngine(dictionary)

    suspend fun warmSuggestions() {
        val words = dao.topUserWords().associate { it.word to it.useCount }
        val bigrams = dao.topBigrams().associate { (it.previousWord to it.nextWord) to it.useCount }
        suggestions.replaceUserWords(words)
        suggestions.replaceBigrams(bigrams)
    }

    suspend fun recordCommittedWord(word: String, previousWord: String?) {
        val normalized = word.trim().lowercase()
        if (normalized.isEmpty()) return
        val now = System.currentTimeMillis()
        suggestions.learnWord(normalized)
        val existing = dao.topUserWords(limit = 50_000).find { it.word == normalized }
        dao.upsertUserWord(
            UserWordEntity(
                word = normalized,
                useCount = (existing?.useCount ?: 0L) + 1L,
                lastUsedAt = now,
            ),
        )
        if (!previousWord.isNullOrBlank()) {
            val prev = previousWord.trim().lowercase()
            suggestions.learnBigram(prev, normalized)
            val existingBigram = dao.topBigrams(limit = 50_000)
                .find { it.previousWord == prev && it.nextWord == normalized }
            dao.upsertBigram(
                LearnedBigramEntity(
                    previousWord = prev,
                    nextWord = normalized,
                    useCount = (existingBigram?.useCount ?: 0L) + 1L,
                ),
            )
        }
    }

    suspend fun learnedWordCount(): Int = dao.userWordCount()
}
