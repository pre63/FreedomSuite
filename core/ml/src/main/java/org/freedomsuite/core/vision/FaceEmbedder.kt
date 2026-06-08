package org.freedomsuite.core.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.sqrt

internal object FaceEmbedder {
    private const val GRID = 16
    private const val EMBEDDING_DIM = GRID * GRID + GRID * GRID / 4 + 8

    fun embed(bitmap: Bitmap, box: RectF, landmarks: FloatArray? = null): FloatArray {
        val aligned = alignFace(bitmap, box, landmarks)
        val gray = toGrayGrid(aligned, GRID)
        if (aligned !== bitmap) aligned.recycle()

        val lbp = localBinaryPattern(gray, GRID)
        val colorHist = colorHistogram(bitmap, box)
        val embedding = FloatArray(EMBEDDING_DIM)
        gray.copyInto(embedding, 0)
        lbp.copyInto(embedding, gray.size)
        colorHist.copyInto(embedding, gray.size + lbp.size)
        return l2Normalize(embedding)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot.coerceIn(-1f, 1f)
    }

    private fun alignFace(bitmap: Bitmap, box: RectF, landmarks: FloatArray?): Bitmap {
        val crop = cropBox(bitmap, box)
        if (landmarks == null || landmarks.size < 10) {
            return Bitmap.createScaledBitmap(crop, GRID * 4, GRID * 4, true).also {
                if (crop !== bitmap && crop !== it) crop.recycle()
            }
        }
        val leftEyeX = (landmarks[0] + landmarks[2]) / 2f * bitmap.width
        val leftEyeY = (landmarks[1] + landmarks[3]) / 2f * bitmap.height
        val rightEyeX = (landmarks[4] + landmarks[6]) / 2f * bitmap.width
        val rightEyeY = (landmarks[5] + landmarks[7]) / 2f * bitmap.height
        val angle = Math.toDegrees(
            kotlin.math.atan2(
                (rightEyeY - leftEyeY).toDouble(),
                (rightEyeX - leftEyeX).toDouble(),
            ),
        ).toFloat()
        val matrix = Matrix()
        matrix.postTranslate(-crop.width / 2f, -crop.height / 2f)
        matrix.postRotate(angle)
        matrix.postTranslate(crop.width / 2f, crop.height / 2f)
        val rotated = Bitmap.createBitmap(crop, 0, 0, crop.width, crop.height, matrix, true)
        val scaled = Bitmap.createScaledBitmap(rotated, GRID * 4, GRID * 4, true)
        if (crop !== bitmap && crop !== rotated) crop.recycle()
        if (rotated !== scaled) rotated.recycle()
        return scaled
    }

    private fun cropBox(bitmap: Bitmap, box: RectF): Bitmap {
        val left = (box.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (box.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (box.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (box.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun toGrayGrid(bitmap: Bitmap, grid: Int): FloatArray {
        val scaled = if (bitmap.width == grid && bitmap.height == grid) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, grid, grid, true)
        }
        val values = FloatArray(grid * grid)
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                val pixel = scaled.getPixel(x, y)
                values[y * grid + x] = (
                    Color.red(pixel) * 0.299f +
                        Color.green(pixel) * 0.587f +
                        Color.blue(pixel) * 0.114f
                    ) / 255f
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        return values
    }

    private fun localBinaryPattern(gray: FloatArray, grid: Int): FloatArray {
        val block = grid / 4
        val out = FloatArray(block * block)
        for (by in 0 until block) {
            for (bx in 0 until block) {
                var code = 0
                val cy = by * 4 + 2
                val cx = bx * 4 + 2
                val center = gray[cy * grid + cx]
                var bit = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val ny = (cy + dy).coerceIn(0, grid - 1)
                        val nx = (cx + dx).coerceIn(0, grid - 1)
                        if (gray[ny * grid + nx] >= center) code = code or (1 shl bit)
                        bit++
                    }
                }
                out[by * block + bx] = code / 255f
            }
        }
        return out
    }

    private fun colorHistogram(bitmap: Bitmap, box: RectF): FloatArray {
        val crop = cropBox(bitmap, box)
        val scaled = Bitmap.createScaledBitmap(crop, 8, 8, true)
        if (crop !== bitmap) crop.recycle()
        val hist = FloatArray(8)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val pixel = scaled.getPixel(x, y)
                val bucket = ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3) * 8 / 256
                hist[bucket.coerceIn(0, 7)] += 1f
            }
        }
        scaled.recycle()
        return l2Normalize(hist)
    }

    private fun l2Normalize(values: FloatArray): FloatArray {
        var sum = 0f
        for (v in values) sum += v * v
        val norm = sqrt(sum).takeIf { it > 0f } ?: return values
        return FloatArray(values.size) { i -> values[i] / norm }
    }
}
