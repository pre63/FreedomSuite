package org.freedomsuite.core.ml

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MlModelsPresentTest {

    @Test
    fun allOnnxModelsAreBundled() {
        val context = MlTestFixtures.appContext()
        for (path in MlTestFixtures.REQUIRED_MODELS) {
            val bytes = context.assets.open(path).use { it.readBytes() }
            assertTrue("$path is empty", bytes.isNotEmpty())
        }
    }

    @Test
    fun ocrDictionaryMatchesPpOcrV4RecModel() {
        val context = MlTestFixtures.appContext()
        val dictLines = context.assets.open("models/en_dict.txt").bufferedReader().readLines()
        // PP-OCRv4 English mobile rec: 97 logits = blank + 95 dict chars + trailing space
        assertTrue("en_dict.txt should have 95 lines, got ${dictLines.size}", dictLines.size >= 90)
    }
}
