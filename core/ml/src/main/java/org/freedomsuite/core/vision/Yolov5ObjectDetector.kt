package org.freedomsuite.core.vision

import android.content.Context
import android.graphics.Bitmap
import org.freedomsuite.core.ml.onnx.BitmapTensor
import org.freedomsuite.core.ml.onnx.OnnxModel
import kotlin.math.max
import kotlin.math.min

internal class Yolov5ObjectDetector(context: Context) : AutoCloseable {
    private val model = OnnxModel(context, "models/yolov5n.onnx")
    private val labels: List<String>
    private val inputSize = 640

    init {
        labels = context.assets.open("models/coco_labels.txt").bufferedReader().readLines()
    }

    fun detect(bitmap: Bitmap): List<DetectedObject> {
        val tensor = BitmapTensor.toNchwFloat(bitmap, inputSize, inputSize, swapRb = false)
        val input = model.createInput(longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()), tensor)
        val output = model.runFirstOutput(input) as Array<Array<FloatArray>>
        input.close()
        return parse(output[0])
    }

    private fun parse(predictions: Array<FloatArray>): List<DetectedObject> {
        val candidates = mutableListOf<Pair<String, Float>>()
        for (row in predictions) {
            if (row.size < 85) continue
            var bestClass = -1
            var bestScore = 0f
            for (classIdx in 0 until 80) {
                val score = row[4 + classIdx]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = classIdx
                }
            }
            if (bestScore < SCORE_THRESHOLD) continue
            val label = labels.getOrNull(bestClass)?.takeIf { it != "???" } ?: continue
            candidates += label to bestScore
        }
        return nonMaxByLabel(candidates)
    }

    private fun nonMaxByLabel(candidates: List<Pair<String, Float>>): List<DetectedObject> {
        return candidates
            .groupBy { it.first }
            .map { (label, scores) -> DetectedObject(label = label, score = scores.maxOf { it.second }) }
            .sortedByDescending { it.score }
            .take(MAX_OBJECTS)
    }

    override fun close() = model.close()

    companion object {
        private const val SCORE_THRESHOLD = 0.35f
        private const val MAX_OBJECTS = 16
    }
}
