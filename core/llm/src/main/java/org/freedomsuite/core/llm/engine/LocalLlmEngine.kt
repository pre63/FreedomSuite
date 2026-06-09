package org.freedomsuite.core.llm.engine

import org.freedomsuite.core.llm.LlmRequest
import org.freedomsuite.core.llm.LlmModelState

interface LocalLlmEngine {
    fun modelState(): LlmModelState

    suspend fun generate(request: LlmRequest): Result<String>
}
