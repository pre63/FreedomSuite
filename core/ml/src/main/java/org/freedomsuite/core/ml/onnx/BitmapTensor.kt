package org.freedomsuite.core.ml.onnx

import android.graphics.Bitmap
import android.graphics.Color

internal object BitmapTensor {
    fun toNchwFloat(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        mean: FloatArray = floatArrayOf(0f, 0f, 0f),
        std: FloatArray = floatArrayOf(1f, 1f, 1f),
        scale: Float = 1f / 255f,
        swapRb: Boolean = true,
    ): FloatArray {
        val scaled = if (bitmap.width == width && bitmap.height == height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) scaled.recycle()

        val data = FloatArray(3 * width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()
            val channels = if (swapRb) floatArrayOf(b, g, r) else floatArrayOf(r, g, b)
            for (c in 0..2) {
                data[c * width * height + i] = (channels[c] * scale - mean[c]) / std[c]
            }
        }
        return data
    }

    fun toNchwFloat16(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        mean: FloatArray = floatArrayOf(0f, 0f, 0f),
        std: FloatArray = floatArrayOf(1f, 1f, 1f),
        scale: Float = 1f / 255f,
        swapRb: Boolean = true,
    ): ShortArray {
        val floats = toNchwFloat(bitmap, width, height, mean, std, scale, swapRb)
        return ShortArray(floats.size) { idx -> floatToHalf(floats[idx]) }
    }

    private fun floatToHalf(value: Float): Short {
        val bits = java.lang.Float.floatToIntBits(value)
        val sign = (bits ushr 16) and 0x8000
        var mantissa = bits and 0x007FFFFF
        var exp = (bits ushr 23) and 0xFF
        if (exp == 0xFF) {
            return (sign or 0x7C00 or (if (mantissa != 0) 0x0200 else 0)).toShort()
        }
        if (exp == 0) {
            return sign.toShort()
        }
        exp = exp - 127 + 15
        if (exp >= 0x1F) {
            return (sign or 0x7C00).toShort()
        }
        if (exp <= 0) {
            mantissa = mantissa or 0x00800000
            val shift = 1 - exp
            mantissa = mantissa ushr (shift + 13)
            return (sign or mantissa).toShort()
        }
        return (sign or (exp shl 10) or (mantissa ushr 13)).toShort()
    }

    fun resizeKeepAspect(
        bitmap: Bitmap,
        limitSideLen: Int,
        limitType: String = "min",
        roundTo: Int = 32,
    ): Pair<Bitmap, Float> {
        val h = bitmap.height
        val w = bitmap.width
        val ratio = when (limitType) {
            "max" -> if (maxOf(h, w) > limitSideLen) limitSideLen.toFloat() / maxOf(h, w) else 1f
            else -> if (minOf(h, w) < limitSideLen) limitSideLen.toFloat() / minOf(h, w) else 1f
        }
        var resizeH = (h * ratio).toInt().coerceAtLeast(roundTo)
        var resizeW = (w * ratio).toInt().coerceAtLeast(roundTo)
        resizeH = ((resizeH + roundTo / 2) / roundTo) * roundTo
        resizeW = ((resizeW + roundTo / 2) / roundTo) * roundTo
        val scaled = Bitmap.createScaledBitmap(bitmap, resizeW, resizeH, true)
        return scaled to ratio
    }
}
