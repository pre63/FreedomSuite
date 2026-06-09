package org.freedomsuite.core.llm

import android.content.Context
import java.io.File

/**
 * Locates the on-device GenAI model directory.
 *
 * Model files are **not** bundled in git. Run `./scripts/fetch-llm-model.sh` then
 * `make push-llm-model` (or copy into app files dir) before first local inference.
 */
class LlmModelManager(private val context: Context) {
    private val modelRoot: File
        get() = File(context.filesDir, "llm/model")

    fun modelDirectory(): File = modelRoot

    fun state(): LlmModelState {
        if (!GenAiRuntime.isNativeAvailable()) {
            return LlmModelState.RUNTIME_UNAVAILABLE
        }
        val config = File(modelRoot, "genai_config.json")
        val weights = File(modelRoot, "model.onnx.data")
        return if (config.isFile && (File(modelRoot, "model.onnx").isFile || weights.isFile)) {
            LlmModelState.READY
        } else {
            LlmModelState.NOT_INSTALLED
        }
    }

    fun installHint(): String =
        when (state()) {
            LlmModelState.READY -> "On-device model ready."
            LlmModelState.RUNTIME_UNAVAILABLE ->
                "GenAI runtime missing. Run: make fetch-llm"
            LlmModelState.NOT_INSTALLED ->
                "Model not on device. On dev machine: make fetch-llm && make push-llm-model"
        }
}
