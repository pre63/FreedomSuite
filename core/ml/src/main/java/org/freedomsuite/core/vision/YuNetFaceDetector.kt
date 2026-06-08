package org.freedomsuite.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.freedomsuite.core.ml.onnx.BitmapTensor
import org.freedomsuite.core.ml.onnx.OnnxModel
import kotlin.math.max
import kotlin.math.min

internal data class YuNetFace(
    val box: RectF,
    val score: Float,
    val landmarks: FloatArray,
)

internal class YuNetFaceDetector(context: Context) : AutoCloseable {
    private val model = OnnxModel(context, "models/yunet.onnx")
    private val inputWidth = 320
    private val inputHeight = 320

    fun detect(bitmap: Bitmap): List<YuNetFace> {
        val tensor = BitmapTensor.toNchwFloat(bitmap, inputWidth, inputHeight)
        val input = model.createInput(longArrayOf(1, 3, inputHeight.toLong(), inputWidth.toLong()), tensor)
        val output = model.runFirstOutput(input) as Array<Array<FloatArray>>
        input.close()
        return parse(output, bitmap.width, bitmap.height)
    }

    private fun parse(output: Array<Array<FloatArray>>, srcW: Int, srcH: Int): List<YuNetFace> {
        val faces = mutableListOf<YuNetFace>()
        for (row in output[0]) {
            if (row.size < 15) continue
            val score = row[14]
            if (score < SCORE_THRESHOLD) continue
            val x = row[0] / inputWidth
            val y = row[1] / inputHeight
            val w = row[2] / inputWidth
            val h = row[3] / inputHeight
            val rect = RectF(
                (x).coerceIn(0f, 1f),
                (y).coerceIn(0f, 1f),
                (x + w).coerceIn(0f, 1f),
                (y + h).coerceIn(0f, 1f),
            )
            if (rect.width() < 0.02f || rect.height() < 0.02f) continue
            val landmarks = FloatArray(10) { idx ->
                val value = row[4 + idx]
                if (idx % 2 == 0) value / inputWidth else value / inputHeight
            }
            faces += YuNetFace(rect, score, landmarks)
        }
        return nonMaxSuppression(faces)
    }

    private fun nonMaxSuppression(faces: List<YuNetFace>): List<YuNetFace> {
        val sorted = faces.sortedByDescending { it.score }
        val kept = mutableListOf<YuNetFace>()
        for (face in sorted) {
            if (kept.none { overlap(it.box, face.box) > NMS_THRESHOLD }) {
                kept += face
            }
            if (kept.size >= MAX_FACES) break
        }
        return kept
    }

    private fun overlap(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val union = a.width() * a.height() + b.width() * b.height() - intersection
        return if (union > 0f) intersection / union else 0f
    }

    override fun close() = model.close()

    companion object {
        private const val SCORE_THRESHOLD = 0.6f
        private const val NMS_THRESHOLD = 0.3f
        private const val MAX_FACES = 8
    }
}
