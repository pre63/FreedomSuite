package org.freedomsuite.core.llm

interface LlmBackend {
    val id: String
    val displayName: String

    suspend fun isAvailable(): Boolean

    suspend fun complete(request: LlmRequest): LlmResponse
}
