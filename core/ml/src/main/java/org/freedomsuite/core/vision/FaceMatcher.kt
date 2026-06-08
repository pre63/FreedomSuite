package org.freedomsuite.core.vision

object FaceMatcher {
    private const val SIMILARITY_THRESHOLD = 0.82f

    fun findSimilar(
        sourceEmbeddings: List<FloatArray>,
        candidates: List<Pair<String, List<FloatArray>>>,
        excludeFileId: String? = null,
        limit: Int = 12,
    ): List<SimilarFaceMatch> {
        if (sourceEmbeddings.isEmpty()) return emptyList()
        val matches = mutableListOf<SimilarFaceMatch>()
        for ((fileId, embeddings) in candidates) {
            if (fileId == excludeFileId) continue
            val best = bestSimilarity(sourceEmbeddings, embeddings)
            if (best >= SIMILARITY_THRESHOLD) {
                matches += SimilarFaceMatch(fileId = fileId, similarity = best)
            }
        }
        return matches.sortedByDescending { it.similarity }.take(limit)
    }

    private fun bestSimilarity(a: List<FloatArray>, b: List<FloatArray>): Float {
        var best = 0f
        for (left in a) {
            for (right in b) {
                val score = FaceEmbedder.cosineSimilarity(left, right)
                if (score > best) best = score
            }
        }
        return best
    }
}
