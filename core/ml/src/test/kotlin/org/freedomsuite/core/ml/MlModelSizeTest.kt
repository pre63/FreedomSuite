package org.freedomsuite.core.ml

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MlModelSizeTest {

    @Before
    fun requireModels() {
        MlTestFixtures.assumeModelsPresent()
    }

    @Test
    fun totalOnnxBundleIsWithinSanityCap() {
        val totalBytes = MlTestFixtures.totalOnnxBytes()
        val totalMb = totalBytes / (1024.0 * 1024.0)
        // Sanity only — post-quantization budget enforced by scripts/ml-integration-test.py
        assertTrue(
            "ONNX bundle is ${"%.1f".format(totalMb)} MB; expected <= 20 MB",
            totalBytes <= 20L * 1024 * 1024,
        )
    }
}
