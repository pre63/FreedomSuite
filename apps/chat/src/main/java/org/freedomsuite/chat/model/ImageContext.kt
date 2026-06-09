package org.freedomsuite.chat.model

/**
 * Textual summary derived locally from a user-provided image.
 *
 * Important privacy property: raw image bytes should never leave the device.
 * Only this summary is eligible to be sent to a remote LLM backend.
 */
data class ImageContext(
    val labels: List<String> = emptyList(),
    val facesCount: Int = 0,
    val ocrTextPreview: String = "",
)

