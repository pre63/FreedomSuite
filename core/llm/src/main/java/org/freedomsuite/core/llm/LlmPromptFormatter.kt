package org.freedomsuite.core.llm

import org.json.JSONArray
import org.json.JSONObject

object LlmPromptFormatter {
    /**
     * JSON chat array for ONNX Runtime GenAI `Tokenizer.applyChatTemplate`.
     */
    fun toChatTemplateJson(request: LlmRequest): String {
        val messages = request.messages
        val lastUserIndex = messages.indexOfLast { it.role == LlmRole.USER }
        val trailingVision = request.visionContext

        val array = JSONArray()
        if (request.systemPrompt.isNotBlank()) {
            array.put(
                JSONObject().apply {
                    put("role", LlmRole.SYSTEM.wireName)
                    put("content", request.systemPrompt)
                },
            )
        }

        messages.forEachIndexed { index, message ->
            var content = message.content
            val vision = when {
                message.visionContext != null -> message.visionContext
                index == lastUserIndex && trailingVision != null -> trailingVision
                else -> null
            }
            if (vision != null) {
                val block = vision.toPromptBlock()
                if (block.isNotBlank()) {
                    content = buildString {
                        append(content)
                        if (content.isNotBlank()) append("\n\n")
                        append("VisionContext: ").append(block)
                    }
                }
            }
            array.put(
                JSONObject().apply {
                    put("role", message.role.wireName)
                    put("content", content)
                },
            )
        }
        return array.toString()
    }

    fun trimHistory(messages: List<LlmMessage>, maxMessages: Int = 24): List<LlmMessage> {
        if (messages.size <= maxMessages) return messages
        return messages.takeLast(maxMessages)
    }
}
