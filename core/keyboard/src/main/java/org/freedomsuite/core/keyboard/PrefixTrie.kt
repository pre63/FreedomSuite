package org.freedomsuite.core.keyboard

internal class PrefixTrie {
    private val root = TrieNode()

    fun insert(word: String, frequency: Long) {
        if (word.isEmpty()) return
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        node.word = word
        node.frequency = maxOf(node.frequency, frequency)
    }

    fun prefixMatches(prefix: String, limit: Int = 8): List<WordCandidate> {
        if (prefix.isEmpty()) return emptyList()
        var node = root
        for (ch in prefix) {
            node = node.children[ch] ?: return emptyList()
        }
        val results = mutableListOf<WordCandidate>()
        collect(node, results, limit)
        return results.sortedByDescending { it.frequency }
    }

    private fun collect(node: TrieNode, out: MutableList<WordCandidate>, limit: Int) {
        if (out.size >= limit) return
        if (node.word != null) {
            out += WordCandidate(node.word!!, node.frequency)
        }
        for (child in node.children.values.sortedByDescending { it.frequency }) {
            collect(child, out, limit)
            if (out.size >= limit) break
        }
    }

    fun contains(word: String): Boolean {
        var node = root
        for (ch in word) {
            node = node.children[ch] ?: return false
        }
        return node.word != null
    }

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var word: String? = null
        var frequency: Long = 0
    }
}

data class WordCandidate(
    val word: String,
    val frequency: Long,
)
