package org.freedomsuite.core.llm

import org.freedomsuite.core.llm.engine.GenAiLocalLlmEngine
import org.freedomsuite.core.llm.engine.LocalLlmEngine

/**
 * Always-present local inference path. This is the foundation of Freedom LLM —
 * remote providers are optional overlays.
 */
class OnDeviceLlmBackend(
    internal val engine: LocalLlmEngine,
) : LlmBackend {
    override val id: String = "on_device"
    override val displayName: String = "On-device (ONNX GenAI)"

    fun modelState(): LlmModelState = engine.modelState()

    override suspend fun isAvailable(): Boolean =
        modelState() == LlmModelState.READY

    override suspend fun complete(request: LlmRequest): LlmResponse {
        val result = engine.generate(request)
        return result.fold(
            onSuccess = { text ->
                LlmResponse(
                    text = text.ifBlank { "…" },
                    source = LlmSource.ON_DEVICE,
                )
            },
            onFailure = { error ->
                LlmResponse(
                    text = error.message ?: "On-device model unavailable.",
                    source = LlmSource.ON_DEVICE,
                    isError = true,
                )
            },
        )
    }

    companion object {
        fun create(context: android.content.Context): OnDeviceLlmBackend =
            OnDeviceLlmBackend(GenAiLocalLlmEngine(context))
    }
}
