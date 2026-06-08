package org.freedomsuite.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.freedomsuite.core.ml.onnx.BitmapTensor
import org.freedomsuite.core.ml.onnx.OnnxModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal class PpOcrEngine(context: Context) : AutoCloseable {
    private val detModel = OnnxModel(context, "models/ocr_det.onnx")
    private val recModel = OnnxModel(context, "models/ocr_rec.onnx")
    private val characters: List<String>

    init {
        val dict = context.assets.open("models/en_dict.txt").bufferedReader().readLines()
        characters = buildList {
            add("blank")
            addAll(dict)
            add(" ")
        }
    }

    fun recognize(bitmap: Bitmap): OcrResult {
        val boxes = detectTextRegions(bitmap)
        val lines = if (boxes.isEmpty()) {
            listOfNotNull(recognizeCrop(bitmap))
        } else {
            boxes.mapNotNull { box -> recognizeCrop(cropBox(bitmap, box)) }
        }
        val filtered = lines.map { it.trim() }.filter { it.isNotEmpty() }
        return OcrResult(text = filtered.joinToString("\n"), lines = filtered)
    }

    private fun detectTextRegions(bitmap: Bitmap): List<IntArray> {
        val (resized, _) = BitmapTensor.resizeKeepAspect(bitmap, limitSideLen = 960, limitType = "min")
        val tensor = BitmapTensor.toNchwFloat(
            resized,
            resized.width,
            resized.height,
            mean = floatArrayOf(0.5f, 0.5f, 0.5f),
            std = floatArrayOf(0.5f, 0.5f, 0.5f),
            swapRb = false,
        )
        val input = detModel.createInput(
            longArrayOf(1, 3, resized.height.toLong(), resized.width.toLong()),
            tensor,
        )
        val pred = detModel.runFirstOutput(input) as Array<Array<Array<FloatArray>>>
        input.close()
        val map = pred[0][0]
        val boxes = DbPostProcess.extractBoxes(map, bitmap.width, bitmap.height)
        if (resized !== bitmap) resized.recycle()
        return boxes
    }

    private fun recognizeCrop(crop: Bitmap): String {
        val imgH = 48
        val maxRatio = 8f
        val ratio = crop.width.toFloat() / crop.height.toFloat()
        val maxW = (imgH * maxRatio).toInt()
        val resizedW = min((ceil(imgH * ratio)).toInt(), maxW).coerceAtLeast(8)
        val scaled = Bitmap.createScaledBitmap(crop, resizedW, imgH, true)
        val imgW = max((imgH * maxRatio).toInt(), resizedW)
        val tensor = FloatArray(3 * imgH * imgW)
        val pixels = IntArray(resizedW * imgH)
        scaled.getPixels(pixels, 0, resizedW, 0, 0, resizedW, imgH)
        if (scaled !== crop) scaled.recycle()

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (Color.red(pixel) / 255f - 0.5f) / 0.5f
            val g = (Color.green(pixel) / 255f - 0.5f) / 0.5f
            val b = (Color.blue(pixel) / 255f - 0.5f) / 0.5f
            val row = i / resizedW
            val col = i % resizedW
            tensor[0 * imgH * imgW + row * imgW + col] = r
            tensor[1 * imgH * imgW + row * imgW + col] = g
            tensor[2 * imgH * imgW + row * imgW + col] = b
        }

        val input = recModel.createInput(longArrayOf(1, 3, imgH.toLong(), imgW.toLong()), tensor)
        val preds = recModel.runFirstOutput(input) as Array<Array<FloatArray>>
        input.close()
        return decodeCtc(preds[0])
    }

    private fun decodeCtc(timeSteps: Array<FloatArray>): String {
        val ignored = setOf(0)
        val chars = StringBuilder()
        var prev = -1
        var scoreSum = 0f
        var scoreCount = 0
        for (step in timeSteps) {
            var bestIdx = 0
            var bestScore = step[0]
            for (i in 1 until step.size) {
                if (step[i] > bestScore) {
                    bestScore = step[i]
                    bestIdx = i
                }
            }
            if (!ignored.contains(bestIdx) && bestIdx != prev) {
                val ch = characters.getOrNull(bestIdx).orEmpty()
                if (ch.isNotEmpty() && ch != "blank") chars.append(ch)
                scoreSum += bestScore
                scoreCount++
            }
            prev = bestIdx
        }
        return chars.toString()
    }

    private fun cropBox(bitmap: Bitmap, box: IntArray): Bitmap {
        val left = box[0].coerceIn(0, bitmap.width - 1)
        val top = box[1].coerceIn(0, bitmap.height - 1)
        val right = box[2].coerceIn(left + 1, bitmap.width)
        val bottom = box[3].coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    override fun close() {
        detModel.close()
        recModel.close()
    }
}

private object DbPostProcess {
    private const val THRESH = 0.3f
    private const val BOX_THRESH = 0.5f
    private const val UNCLIP_RATIO = 1.6f
    private const val MIN_SIZE = 3

    fun extractBoxes(
        pred: Array<FloatArray>,
        srcW: Int,
        srcH: Int,
    ): List<IntArray> {
        val h = pred.size
        val w = pred[0].size
        val mask = Array(h) { y -> BooleanArray(w) { x -> pred[y][x] > THRESH } }
        val visited = Array(h) { BooleanArray(w) }
        val boxes = mutableListOf<IntArray>()
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (!mask[y][x] || visited[y][x]) continue
                val component = floodFill(mask, visited, x, y)
                if (component.size < MIN_SIZE * MIN_SIZE) continue
                var minX = w
                var minY = h
                var maxX = 0
                var maxY = 0
                var scoreSum = 0f
                for ((cx, cy) in component) {
                    minX = min(minX, cx)
                    minY = min(minY, cy)
                    maxX = max(maxX, cx)
                    maxY = max(maxY, cy)
                    scoreSum += pred[cy][cx]
                }
                val score = scoreSum / component.size
                if (score < BOX_THRESH) continue
                val box = expandBox(minX, minY, maxX, maxY, w, h)
                val mapped = intArrayOf(
                    (box[0] / w * srcW).toInt().coerceIn(0, srcW - 1),
                    (box[1] / h * srcH).toInt().coerceIn(0, srcH - 1),
                    (box[2] / w * srcW).toInt().coerceIn(1, srcW),
                    (box[3] / h * srcH).toInt().coerceIn(1, srcH),
                )
                if (mapped[2] - mapped[0] > 3 && mapped[3] - mapped[1] > 3) {
                    boxes += mapped
                }
            }
        }
        return boxes.sortedWith(compareBy<IntArray> { it[1] }.thenBy { it[0] })
    }

    private fun floodFill(mask: Array<BooleanArray>, visited: Array<BooleanArray>, sx: Int, sy: Int): List<Pair<Int, Int>> {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(sx to sy)
        val pixels = mutableListOf<Pair<Int, Int>>()
        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()
            if (x !in mask[0].indices || y !in mask.indices) continue
            if (!mask[y][x] || visited[y][x]) continue
            visited[y][x] = true
            pixels += x to y
            stack.add(x + 1 to y)
            stack.add(x - 1 to y)
            stack.add(x to y + 1)
            stack.add(x to y - 1)
        }
        return pixels
    }

    private fun expandBox(minX: Int, minY: Int, maxX: Int, maxY: Int, w: Int, h: Int): IntArray {
        val boxW = maxX - minX + 1
        val boxH = maxY - minY + 1
        val area = boxW * boxH
        val perimeter = 2f * (boxW + boxH)
        val distance = (sqrt(area.toFloat()) * UNCLIP_RATIO).toInt().coerceAtLeast(1)
        return intArrayOf(
            (minX - distance).coerceAtLeast(0),
            (minY - distance).coerceAtLeast(0),
            (maxX + distance).coerceAtMost(w - 1),
            (maxY + distance).coerceAtMost(h - 1),
        )
    }
}
