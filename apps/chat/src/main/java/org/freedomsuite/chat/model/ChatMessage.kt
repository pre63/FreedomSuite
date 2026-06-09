package org.freedomsuite.chat.model

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val imageContext: ImageContext? = null,
) {
    enum class Role {
        USER,
        ASSISTANT,
    }
}

