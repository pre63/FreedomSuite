package org.freedomsuite.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.freedomsuite.core.ocr.LocalOcrEngine

object LocalVisionEngine {
    private val initMutex = Mutex()
    @Volatile
    private var objectDetector: Yolov5ObjectDetector? = null
    @Volatile
    private var faceDetector: YuNetFaceDetector? = null

    suspend fun indexImage(
        context: Context,
        imageBytes: ByteArray,
        fileName: String,
        includeOcr: Boolean = true,
    ): Result<VisionIndexResult> = withContext(Dispatchers.Default) {
        runCatching {
            ensureEngines(context.applicationContext)
            val bitmap = decodeBitmap(imageBytes) ?: error("Could not decode image")
            try {
                val objects = objectDetector!!.detect(bitmap)
                val faceBoxes = faceDetector!!.detect(bitmap)
                val faces = faceBoxes.map { face ->
                    DetectedFace(
                        left = face.box.left,
                        top = face.box.top,
                        right = face.box.right,
                        bottom = face.box.bottom,
                        score = face.score,
                        embedding = FaceEmbedder.embed(bitmap, face.box, face.landmarks),
                    )
                }
                val ocrText = if (includeOcr) {
                    LocalOcrEngine.recognize(context, imageBytes).getOrNull()?.text.orEmpty()
                } else {
                    ""
                }
                val searchBlob = VisionSearch.buildSearchBlob(objects, ocrText, fileName)
                VisionIndexResult(
                    objects = objects,
                    faces = faces,
                    ocrText = ocrText,
                    searchBlob = searchBlob,
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    private suspend fun ensureEngines(context: Context) {
        if (objectDetector != null && faceDetector != null) return
        initMutex.withLock {
            if (objectDetector == null) objectDetector = Yolov5ObjectDetector(context)
            if (faceDetector == null) faceDetector = YuNetFaceDetector(context)
        }
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w > 1600 || h > 1600) {
            sample *= 2
            w /= 2
            h /= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
