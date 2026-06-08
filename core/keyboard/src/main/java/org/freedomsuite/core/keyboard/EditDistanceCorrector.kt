package org.freedomsuite.core.keyboard

internal object EditDistanceCorrector {
    fun candidates(
        typed: String,
        dictionary: FrequencyDictionary,
        maxEdits: Int = 2,
        limit: Int = 6,
    ): List<WordCandidate> {
        if (typed.length < 2) return emptyList()
        val lower = typed.lowercase()
        if (dictionary.contains(lower)) return emptyList()

        val seen = mutableSetOf<String>()
        val results = mutableListOf<WordCandidate>()

        fun add(word: String) {
            if (word == lower || word in seen) return
            seen += word
            val freq = dictionary.frequency(word)
            if (freq > 0) results += WordCandidate(word, freq)
        }

        // One edit away
        for (i in lower.indices) {
            add(lower.removeRange(i, i + 1))
            if (i > 0) add(swap(lower, i - 1, i))
        }
        for (i in 0..lower.length) {
            for (ch in 'a'..'z') {
                add(lower.substring(0, i) + ch + lower.substring(i))
                if (i < lower.length) add(lower.substring(0, i) + ch + lower.substring(i + 1))
            }
        }

        if (maxEdits >= 2 && results.size < limit) {
            val firstPass = results.map { it.word }.toSet()
            for (word in firstPass) {
                for (i in word.indices) {
                    add(word.removeRange(i, i + 1))
                }
            }
        }

        return results
            .sortedWith(compareByDescending<WordCandidate> { it.frequency }.thenBy { it.word.length })
            .take(limit)
    }

    private fun swap(value: String, a: Int, b: Int): String {
        if (a == b) return value
        val chars = value.toCharArray()
        val tmp = chars[a]
        chars[a] = chars[b]
        chars[b] = tmp
        return String(chars)
    }
}
