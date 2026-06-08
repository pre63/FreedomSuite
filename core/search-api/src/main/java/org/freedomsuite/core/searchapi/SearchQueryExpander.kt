package org.freedomsuite.core.searchapi

/**
 * Shared query expansion for vision labels, mail, and unified search.
 * Maps natural terms (car, id card) to indexed synonyms (vehicle, license, …).
 */
object SearchQueryExpander {
    private val synonyms = mapOf(
        "girl" to listOf("person", "woman", "female"),
        "boy" to listOf("person", "man", "male"),
        "woman" to listOf("person", "woman"),
        "man" to listOf("person", "man"),
        "kid" to listOf("person", "child"),
        "child" to listOf("person"),
        "id" to listOf("card", "license", "passport", "identification", "id"),
        "id card" to listOf("card", "license", "passport", "identification", "id"),
        "license" to listOf("license", "card", "id"),
        "passport" to listOf("passport", "card", "id"),
        "car" to listOf("car", "vehicle", "automobile", "truck", "bus"),
        "vehicle" to listOf("car", "truck", "bus", "motorcycle", "bicycle"),
        "phone" to listOf("cell phone", "phone", "mobile"),
        "computer" to listOf("laptop", "keyboard", "mouse", "tv"),
        "pet" to listOf("dog", "cat", "bird"),
        "animal" to listOf("dog", "cat", "bird", "horse", "sheep", "cow"),
        "food" to listOf("pizza", "sandwich", "apple", "banana", "cake", "donut"),
        "audio" to listOf("audio", "voice", "recording", "transcript"),
        "voice" to listOf("voice", "audio", "transcript", "dictation"),
    )

    fun expand(query: String): List<String> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()
        val tokens = mutableSetOf(trimmed)
        synonyms[trimmed]?.let { tokens.addAll(it) }
        trimmed.split(Regex("\\s+")).forEach { word ->
            if (word.isNotBlank()) {
                tokens += word
                synonyms[word]?.let { tokens.addAll(it) }
            }
        }
        return tokens.toList()
    }

    fun matchesBlob(blob: String, query: String): Boolean {
        val haystack = blob.lowercase()
        return expand(query).any { term -> haystack.contains(term) }
    }
}
