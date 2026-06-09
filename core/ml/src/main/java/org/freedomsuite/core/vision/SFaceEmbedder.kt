package org.freedomsuite.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.freedomsuite.core.ml.onnx.OnnxModel
import kotlin.math.sqrt

/**
 * SFace (OpenCV Zoo, Apache 2.0) — 128-D face embeddings for similar-face search.
 */
internal class SFaceEmbedder(context: Context) : AutoCloseable {
  private val model = OnnxModel(context, "models/sface.onnx")
  private val inputSize = 112

  fun embed(bitmap: Bitmap, face: YuNetFace): FloatArray {
    val aligned = FaceAlign.alignTo112(bitmap, face.landmarks)
    return try {
      val tensor = toInputTensor(aligned)
      val input = model.createInput(
        longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()),
        tensor,
      )
      val raw = model.runFirstOutput(input) as Array<FloatArray>
      input.close()
      l2Normalize(raw[0])
    } finally {
      if (aligned !== bitmap) aligned.recycle()
    }
  }

  private fun toInputTensor(bitmap: Bitmap): FloatArray {
    val pixels = IntArray(inputSize * inputSize)
    bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
    val data = FloatArray(3 * inputSize * inputSize)
    for (i in pixels.indices) {
      val pixel = pixels[i]
      // OpenCV blobFromImage: RGB, scale 1.0 → 0..255
      val r = Color.red(pixel).toFloat()
      val g = Color.green(pixel).toFloat()
      val b = Color.blue(pixel).toFloat()
      data[i] = r
      data[inputSize * inputSize + i] = g
      data[2 * inputSize * inputSize + i] = b
    }
    return data
  }

  private fun l2Normalize(values: FloatArray): FloatArray {
    var sum = 0f
    for (v in values) sum += v * v
    val norm = sqrt(sum).takeIf { it > 0f } ?: return values
    return FloatArray(values.size) { i -> values[i] / norm }
  }

  override fun close() = model.close()
}
