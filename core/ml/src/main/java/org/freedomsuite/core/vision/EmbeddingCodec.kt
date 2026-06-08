package org.freedomsuite.core.vision

import org.json.JSONArray

object EmbeddingCodec {
    fun encode(faces: List<DetectedFace>): String {
        val root = JSONArray()
        faces.forEach { face ->
            val emb = JSONArray()
            face.embedding.forEach { emb.put(it.toDouble()) }
            root.put(emb)
        }
        return root.toString()
    }

    fun decode(json: String): List<FloatArray> {
        if (json.isBlank()) return emptyList()
        val root = JSONArray(json)
        return buildList {
            for (i in 0 until root.length()) {
                val emb = root.getJSONArray(i)
                add(FloatArray(emb.length()) { idx -> emb.getDouble(idx).toFloat() })
            }
        }
    }
}
