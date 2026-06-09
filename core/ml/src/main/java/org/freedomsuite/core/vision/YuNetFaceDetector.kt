package org.freedomsuite.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import ai.onnxruntime.OnnxTensor
import org.freedomsuite.core.ml.onnx.BitmapTensor
import org.freedomsuite.core.ml.onnx.OnnxModel

internal data class YuNetFace(
  val box: RectF,
  val score: Float,
  /** Pixel coordinates in padded input space (x,y per landmark). */
  val landmarks: FloatArray,
)

internal class YuNetFaceDetector(context: Context) : AutoCloseable {
  private val model = OnnxModel(context, "models/yunet.onnx")
  private val outputNames = listOf(
    "cls_8", "cls_16", "cls_32",
    "obj_8", "obj_16", "obj_32",
    "bbox_8", "bbox_16", "bbox_32",
    "kps_8", "kps_16", "kps_32",
  )

  fun detect(bitmap: Bitmap): List<YuNetFace> {
    val padW = ((bitmap.width - 1) / DIVISOR + 1) * DIVISOR
    val padH = ((bitmap.height - 1) / DIVISOR + 1) * DIVISOR
    val padded = if (padW == bitmap.width && padH == bitmap.height) {
      bitmap
    } else {
      Bitmap.createBitmap(padW, padH, Bitmap.Config.ARGB_8888).also { out ->
        Canvas(out).drawBitmap(bitmap, 0f, 0f, null)
      }
    }

    val tensor = BitmapTensor.toNchwFloat(
      padded,
      padW,
      padH,
      scale = 1f,
      swapRb = false,
    )
    val input = model.createInput(longArrayOf(1, 3, padH.toLong(), padW.toLong()), tensor)
    val outputs = runAllOutputs(input)
    input.close()
    if (padded !== bitmap) padded.recycle()

    val mapped = YuNetPostProcess.decode(
      outputs = outputs,
      padW = padW,
      padH = padH,
      srcW = bitmap.width,
      srcH = bitmap.height,
      scoreThreshold = SCORE_THRESHOLD,
      nmsThreshold = NMS_THRESHOLD,
      topK = MAX_FACES,
    )
    return mapped
  }

  @Suppress("UNCHECKED_CAST")
  private fun runAllOutputs(input: OnnxTensor): Map<String, Array<Array<FloatArray>>> {
    val result = model.run(input)
    return try {
      buildMap {
        for (name in outputNames) {
          val value = result.get(name).get().value as Array<Array<FloatArray>>
          put(name, value)
        }
      }
    } finally {
      result.close()
    }
  }

  override fun close() = model.close()

  companion object {
    private const val DIVISOR = 32
    private const val SCORE_THRESHOLD = 0.6f
    private const val NMS_THRESHOLD = 0.3f
    private const val MAX_FACES = 8
  }
}
