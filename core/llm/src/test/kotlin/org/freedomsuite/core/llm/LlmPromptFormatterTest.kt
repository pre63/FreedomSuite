package org.freedomsuite.core.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class LlmPromptFormatterTest {
    @Test
    fun chatTemplateIncludesVisionOnLastUser() {
        val json = LlmPromptFormatter.toChatTemplateJson(
            LlmRequest(
                messages = listOf(
                    LlmMessage(LlmRole.USER, "What is in the photo?"),
                ),
                visionContext = VisionContext(
                    labels = listOf("car", "person"),
                    facesCount = 1,
                    ocrTextPreview = "ABC123",
                ),
            ),
        )
        assertTrue(json.contains("VisionContext"))
        assertTrue(json.contains("car"))
        assertTrue(json.contains("ABC123"))
    }

    @Test
    fun trimHistoryKeepsTail() {
        val messages = (1..30).map { i ->
            LlmMessage(
                role = if (i % 2 == 0) LlmRole.ASSISTANT else LlmRole.USER,
                content = "m$i",
            )
        }
        val trimmed = LlmPromptFormatter.trimHistory(messages, maxMessages = 10)
        assertTrue(trimmed.size == 10)
        assertTrue(trimmed.last().content == "m30")
    }
}
