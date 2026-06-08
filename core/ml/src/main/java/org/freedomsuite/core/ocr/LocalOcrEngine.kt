package org.freedomsuite.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class OcrResult(
    val text: String,
    val lines: List<String>,
)

/**
 * On-device OCR via ONNX Runtime (PP-OCR). No network, no cloud APIs.
 */
object LocalOcrEngine {
    private val initMutex = Mutex()
    @Volatile
    private var engine: PpOcrEngine? = null

    suspend fun recognize(context: Context, imageBytes: ByteArray): Result<OcrResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                val appContext = context.applicationContext
                val ocr = ensureEngine(appContext)
                val bitmap = decodeForOcr(imageBytes) ?: error("Could not decode image")
                try {
                    ocr.recognize(bitmap)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }

    private suspend fun ensureEngine(context: Context): PpOcrEngine {
        engine?.let { return it }
        return initMutex.withLock {
            engine ?: PpOcrEngine(context.applicationContext).also { engine = it }
        }
    }

    private fun decodeForOcr(imageBytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxEdge = 2048)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w > maxEdge || h > maxEdge) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample.coerceAtLeast(1)
    }
}
