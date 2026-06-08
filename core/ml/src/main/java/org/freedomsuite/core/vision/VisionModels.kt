package org.freedomsuite.core.vision

data class DetectedObject(
    val label: String,
    val score: Float,
)

data class DetectedFace(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float,
    val embedding: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedFace) return false
        return left == other.left && top == other.top && right == other.right &&
            bottom == other.bottom && score == other.score && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int = embedding.contentHashCode()
}

data class VisionIndexResult(
    val objects: List<DetectedObject>,
    val faces: List<DetectedFace>,
    val ocrText: String,
    val searchBlob: String,
)

data class SimilarFaceMatch(
    val fileId: String,
    val similarity: Float,
)
