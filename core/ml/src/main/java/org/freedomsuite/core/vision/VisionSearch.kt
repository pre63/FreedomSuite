package org.freedomsuite.core.vision

import org.freedomsuite.core.searchapi.SearchQueryExpander

object VisionSearch {
    fun expandQuery(query: String): List<String> = SearchQueryExpander.expand(query)

    fun matches(searchBlob: String, query: String): Boolean =
        SearchQueryExpander.matchesBlob(searchBlob, query)

    fun buildSearchBlob(
        objects: List<DetectedObject>,
        ocrText: String,
        fileName: String,
    ): String {
        val parts = mutableListOf<String>()
        parts += fileName.lowercase()
        objects.forEach { parts += it.label.lowercase() }
        parts += ocrText.lowercase()
        return parts.joinToString(" ")
    }
}
