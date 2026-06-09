package org.freedomsuite.chat.llm

import android.content.Context
import org.freedomsuite.chat.config.ChatConfigStore
import org.freedomsuite.chat.config.ChatProvider
import org.freedomsuite.chat.model.ChatMessage
import org.freedomsuite.chat.model.ImageContext
import org.freedomsuite.core.llm.FreedomLlmService
import org.freedomsuite.core.llm.LlmMessage
import org.freedomsuite.core.llm.LlmRequest
import org.freedomsuite.core.llm.LlmResponse
import org.freedomsuite.core.llm.LlmRole
import org.freedomsuite.core.llm.LlmRouting
import org.freedomsuite.core.llm.RemoteLlmConfig
import org.freedomsuite.core.llm.VisionContext

object ChatLlmBridge {
    fun createService(context: Context, config: ChatConfigStore): FreedomLlmService {
        val routing = when (config.getProvider()) {
            ChatProvider.LOCAL -> LlmRouting.ON_DEVICE_ONLY
            ChatProvider.GROK -> LlmRouting.REMOTE_WITH_ON_DEVICE_FALLBACK
        }
        val remote = if (routing == LlmRouting.ON_DEVICE_ONLY) {
            null
        } else {
            RemoteLlmConfig(
                baseUrl = config.getGrokBaseUrl().orEmpty(),
                apiKey = config.getGrokApiKey().orEmpty(),
                model = config.getGrokModel(),
                chatPath = config.getGrokChatPath(),
            )
        }
        return FreedomLlmService.create(
            context = context.applicationContext,
            routing = routing,
            remoteConfig = remote,
        )
    }

    fun toLlmRequest(
        messages: List<ChatMessage>,
        trailingVision: ImageContext?,
    ): LlmRequest {
        val llmMessages = messages.map { msg ->
            LlmMessage(
                role = when (msg.role) {
                    ChatMessage.Role.USER -> LlmRole.USER
                    ChatMessage.Role.ASSISTANT -> LlmRole.ASSISTANT
                },
                content = msg.content,
                visionContext = msg.imageContext?.toVisionContext(),
            )
        }
        return LlmRequest(
            messages = llmMessages,
            visionContext = trailingVision?.toVisionContext(),
        )
    }

    private fun ImageContext.toVisionContext(): VisionContext =
        VisionContext(
            labels = labels,
            facesCount = facesCount,
            ocrTextPreview = ocrTextPreview,
        )
}
