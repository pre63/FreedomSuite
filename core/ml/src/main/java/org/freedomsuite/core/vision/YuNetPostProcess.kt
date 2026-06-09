package org.freedomsuite.core.vision

import android.graphics.RectF
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * YuNet decode + NMS (ported from OpenCV FaceDetectorYNImpl::postProcess).
 */
internal object YuNetPostProcess {
  private val STRIDES = intArrayOf(8, 16, 32)

  fun decode(
    outputs: Map<String, Array<Array<FloatArray>>>,
    padW: Int,
    padH: Int,
    srcW: Int,
    srcH: Int,
    scoreThreshold: Float,
    nmsThreshold: Float,
    topK: Int,
  ): List<YuNetFace> {
    val faces = mutableListOf<YuNetFace>()
    for (i in STRIDES.indices) {
      val stride = STRIDES[i]
      val cols = padW / stride
      val rows = padH / stride
      val cls = outputs["cls_$stride"] ?: continue
      val obj = outputs["obj_$stride"] ?: continue
      val bbox = outputs["bbox_$stride"] ?: continue
      val kps = outputs["kps_$stride"] ?: continue
      val clsMap = cls[0]
      val objMap = obj[0]
      val bboxMap = bbox[0]
      val kpsMap = kps[0]

      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val idx = r * cols + c
          val clsScore = clsMap[idx][0].coerceIn(0f, 1f)
          val objScore = objMap[idx][0].coerceIn(0f, 1f)
          val score = sqrt(clsScore * objScore)
          if (score < scoreThreshold) continue

          val cx = (c + bboxMap[idx][0]) * stride
          val cy = (r + bboxMap[idx][1]) * stride
          val w = exp(bboxMap[idx][2].toDouble()).toFloat() * stride
          val h = exp(bboxMap[idx][3].toDouble()).toFloat() * stride
          val x1 = cx - w / 2f
          val y1 = cy - h / 2f

          val landmarks = FloatArray(10)
          for (n in 0 until 5) {
            landmarks[n * 2] = (kpsMap[idx][n * 2] + c) * stride
            landmarks[n * 2 + 1] = (kpsMap[idx][n * 2 + 1] + r) * stride
          }

          faces += YuNetFace(
            box = RectF(
              (x1 / srcW).coerceIn(0f, 1f),
              (y1 / srcH).coerceIn(0f, 1f),
              ((x1 + w) / srcW).coerceIn(0f, 1f),
              ((y1 + h) / srcH).coerceIn(0f, 1f),
            ),
            score = score,
            landmarks = landmarks,
          )
        }
      }
    }

    return nonMaxSuppression(faces, nmsThreshold, topK)
  }

  private fun nonMaxSuppression(
    faces: List<YuNetFace>,
    nmsThreshold: Float,
    topK: Int,
  ): List<YuNetFace> {
    val sorted = faces.sortedByDescending { it.score }
    val kept = mutableListOf<YuNetFace>()
    for (face in sorted) {
      if (kept.none { overlap(it.box, face.box) > nmsThreshold }) {
        kept += face
      }
      if (kept.size >= topK) break
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
}
