package org.freedomsuite.core.llm

internal object GenAiRuntime {
    fun isNativeAvailable(): Boolean = runCatching {
        Class.forName("ai.onnxruntime.genai.Model")
        true
    }.getOrDefault(false)
}
