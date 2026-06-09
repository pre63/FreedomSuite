package org.freedomsuite.core.llm

data class VisionContext(
    val labels: List<String> = emptyList(),
    val facesCount: Int = 0,
    val ocrTextPreview: String = "",
) {
    fun toPromptBlock(): String {
        val labelsPart = labels.take(8).distinct().joinToString(", ").takeIf { it.isNotBlank() }
        val facesPart = facesCount.takeIf { it > 0 }?.let { "faces=$it" }
        val ocrPart = ocrTextPreview.trim().take(400).takeIf { it.isNotBlank() }?.let { "ocr=\"$it\"" }
        return listOfNotNull(labelsPart?.let { "objects=$it" }, facesPart, ocrPart).joinToString(" · ")
    }
}

enum class LlmRole(val wireName: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
}

data class LlmMessage(
    val role: LlmRole,
    val content: String,
    val visionContext: VisionContext? = null,
)

data class LlmRequest(
    val messages: List<LlmMessage>,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int = 384,
    val temperature: Double = 0.7,
    val visionContext: VisionContext? = null,
)

data class LlmResponse(
    val text: String,
    val source: LlmSource,
    val isError: Boolean = false,
)

enum class LlmSource {
    ON_DEVICE,
    REMOTE,
}

enum class LlmRouting {
    /** Always run the on-device engine (foundation path). */
    ON_DEVICE_ONLY,
    /** Prefer remote when configured; fall back to on-device if remote fails. */
    REMOTE_WITH_ON_DEVICE_FALLBACK,
    /** Remote only — still requires on-device stack to be present for offline tooling. */
    REMOTE_ONLY,
}

enum class LlmModelState {
    READY,
    NOT_INSTALLED,
    RUNTIME_UNAVAILABLE,
}

const val DEFAULT_SYSTEM_PROMPT =
    "You are Freedom Chat, a private on-device assistant. Be concise and helpful. " +
        "You may receive VisionContext summaries from local ONNX vision (objects, faces, OCR) — never raw images."
