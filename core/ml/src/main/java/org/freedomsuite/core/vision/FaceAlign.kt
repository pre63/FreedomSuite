package org.freedomsuite.core.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Aligns a face crop to 112×112 using YuNet landmarks (OpenCV SFace template).
 */
internal object FaceAlign {
  private val DST = arrayOf(
    floatArrayOf(38.2946f, 51.6963f),
    floatArrayOf(73.5318f, 51.5014f),
    floatArrayOf(56.0252f, 71.7366f),
    floatArrayOf(41.5493f, 92.3655f),
    floatArrayOf(70.7299f, 92.2041f),
  )

  fun alignTo112(bitmap: Bitmap, landmarks: FloatArray): Bitmap {
    require(landmarks.size >= 10) { "Need 5 landmarks" }
    val src = Array(5) { i -> floatArrayOf(landmarks[i * 2], landmarks[i * 2 + 1]) }
    val matrix = similarityTransform(src)
    val output = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return output
  }

  private fun similarityTransform(src: Array<FloatArray>): Matrix {
    val leftEye = src[0]
    val rightEye = src[1]
    val nose = src[2]
    val angle = atan2(
      (rightEye[1] - leftEye[1]).toDouble(),
      (rightEye[0] - leftEye[0]).toDouble(),
    ).toFloat()
    val eyeDist = hypot((rightEye[0] - leftEye[0]).toDouble(), (rightEye[1] - leftEye[1]).toDouble()).toFloat()
    val dstEyeDist = DST[1][0] - DST[0][0]
    val scale = dstEyeDist / eyeDist.coerceAtLeast(1f)

    val srcCx = (leftEye[0] + rightEye[0] + nose[0]) / 3f
    val srcCy = (leftEye[1] + rightEye[1] + nose[1]) / 3f
    val dstCx = (DST[0][0] + DST[1][0] + DST[2][0]) / 3f
    val dstCy = (DST[0][1] + DST[1][1] + DST[2][1]) / 3f

    val matrix = Matrix()
    matrix.postTranslate(-srcCx, -srcCy)
    matrix.postRotate(Math.toDegrees(angle.toDouble()).toFloat())
    matrix.postScale(scale, scale)
    matrix.postTranslate(dstCx, dstCy)
    return matrix
  }
}
