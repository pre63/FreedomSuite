package org.freedomsuite.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import org.junit.Assume.assumeTrue

object MlTestFixtures {
    val REQUIRED_MODELS = listOf(
        "models/yunet.onnx",
        "models/sface.onnx",
        "models/yolov5n.onnx",
        "models/ocr_det.onnx",
        "models/ocr_rec.onnx",
        "models/en_dict.txt",
        "models/coco_labels.txt",
        "models/MODELS-MANIFEST.json",
    )

    fun appContext(): Context = ApplicationProvider.getApplicationContext()

    fun assumeModelsPresent(context: Context = appContext()) {
        val missing = REQUIRED_MODELS.filter { path ->
            runCatching { context.assets.open(path).use { } }.isFailure
        }
        assumeTrue(
            "ML models missing (${missing.joinToString()}). Run ./scripts/fetch-ml-models.sh",
            missing.isEmpty(),
        )
    }

    fun textBitmap(
        text: String,
        width: Int = 480,
        height: Int = 120,
        textSize: Float = 56f,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(text, 24f, height * 0.7f, paint)
        return bitmap
    }

    fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    fun totalOnnxBytes(context: Context = appContext()): Long =
        REQUIRED_MODELS
            .filter { it.endsWith(".onnx") }
            .sumOf { path ->
                context.assets.openFd(path).use { it.length }
            }
}
