package org.freedomsuite.core.keyboard

import android.content.Context

class FrequencyDictionary(context: Context) {
    private val trie = PrefixTrie()
    private val frequencies = mutableMapOf<String, Long>()

    init {
        context.assets.open("en_50k.txt").bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size != 2) return@forEach
                val word = parts[0].lowercase()
                val freq = parts[1].toLongOrNull() ?: return@forEach
                frequencies[word] = freq
                trie.insert(word, freq)
            }
        }
    }

    fun frequency(word: String): Long = frequencies[word.lowercase()] ?: 0L

    fun contains(word: String): Boolean = trie.contains(word.lowercase())

    fun prefixCompletions(prefix: String, limit: Int = 8): List<WordCandidate> =
        trie.prefixMatches(prefix.lowercase(), limit)
}
