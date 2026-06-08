package org.freedomsuite.core.keyboard

/**
 * Local word prediction: trie prefix completion, edit-distance autocorrect,
 * learned bigrams, and a personal dictionary. No network, no LLM per keystroke.
 */
class SuggestionEngine(
    private val dictionary: FrequencyDictionary,
) {
    data class Suggestions(
        val completions: List<String>,
        val corrections: List<String>,
        val nextWords: List<String>,
    )

    private val userWords = mutableMapOf<String, Long>()
    private val bigrams = mutableMapOf<Pair<String, String>, Long>()

    fun learnWord(word: String) {
        val normalized = normalizeToken(word) ?: return
        userWords[normalized] = (userWords[normalized] ?: 0L) + 1L
    }

    fun learnBigram(previous: String, current: String) {
        val prev = normalizeToken(previous) ?: return
        val cur = normalizeToken(current) ?: return
        val key = prev to cur
        bigrams[key] = (bigrams[key] ?: 0L) + 1L
    }

    fun replaceUserWords(words: Map<String, Long>) {
        userWords.clear()
        userWords.putAll(words.mapKeys { it.key.lowercase() })
    }

    fun replaceBigrams(pairs: Map<Pair<String, String>, Long>) {
        bigrams.clear()
        bigrams.putAll(pairs)
    }

    fun suggest(partialWord: String, previousWord: String?): Suggestions {
        val typed = partialWord.lowercase()
        val completions = mutableListOf<WordCandidate>()

        if (typed.isNotEmpty()) {
            completions += dictionary.prefixCompletions(typed, limit = 6)
            completions += userWords.entries
                .filter { it.key.startsWith(typed) }
                .map { WordCandidate(it.key, it.value + 1_000_000) }
            completions += EditDistanceCorrector.candidates(typed, dictionary, limit = 4)
        }

        val nextWords = if (typed.isEmpty() && !previousWord.isNullOrBlank()) {
            nextWordCandidates(previousWord)
        } else {
            emptyList()
        }

        val rankedCompletions = completions
            .groupBy { it.word }
            .map { (_, group) -> group.maxBy { it.frequency } }
            .sortedByDescending { it.frequency }
            .map { it.word }
            .distinct()
            .take(3)

        val corrections = if (typed.length >= 3 && !dictionary.contains(typed)) {
            EditDistanceCorrector.candidates(typed, dictionary, limit = 3).map { it.word }
        } else {
            emptyList()
        }

        return Suggestions(
            completions = rankedCompletions,
            corrections = corrections.filter { it !in rankedCompletions }.take(2),
            nextWords = nextWords.take(3),
        )
    }

    private fun nextWordCandidates(previousWord: String): List<String> {
        val prev = normalizeToken(previousWord) ?: return emptyList()
        return bigrams.entries
            .filter { it.key.first == prev }
            .sortedByDescending { it.value }
            .map { it.key.second }
            .distinct()
            .take(6)
    }

    private fun normalizeToken(word: String): String? {
        val trimmed = word.trim().lowercase()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { !it.isLetterOrDigit() && it != '\'' && it != '-' }) return null
        return trimmed
    }
}
