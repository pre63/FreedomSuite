package org.freedomsuite.core.llm.engine

import ai.onnxruntime.genai.GeneratorParams
import ai.onnxruntime.genai.SimpleGenAI
import ai.onnxruntime.genai.Tokenizer
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.freedomsuite.core.llm.LlmModelManager
import org.freedomsuite.core.llm.LlmModelState
import org.freedomsuite.core.llm.LlmPromptFormatter
import org.freedomsuite.core.llm.LlmRequest

/**
 * On-device text generation via ONNX Runtime GenAI (SmolLM2 / Phi-3 GenAI packs).
 */
class GenAiLocalLlmEngine(context: Context) : LocalLlmEngine {
    private val appContext = context.applicationContext
    private val modelManager = LlmModelManager(appContext)
    private val mutex = Mutex()

    @Volatile
    private var runtime: SimpleGenAI? = null

    @Volatile
    private var tokenizer: Tokenizer? = null

    override fun modelState(): LlmModelState = modelManager.state()

    override suspend fun generate(request: LlmRequest): Result<String> = withContext(Dispatchers.Default) {
        when (modelManager.state()) {
            LlmModelState.RUNTIME_UNAVAILABLE ->
                return@withContext Result.failure(
                    IllegalStateException(modelManager.installHint()),
                )
            LlmModelState.NOT_INSTALLED ->
                return@withContext Result.failure(
                    IllegalStateException(modelManager.installHint()),
                )
            LlmModelState.READY -> Unit
        }

        mutex.withLock {
            runCatching {
                val genai = ensureRuntime()
                val tok = ensureTokenizer(genai)
                val trimmed = request.copy(
                    messages = LlmPromptFormatter.trimHistory(request.messages),
                )
                val chatJson = LlmPromptFormatter.toChatTemplateJson(trimmed)
                val prompt = tok.applyChatTemplate(chatJson, "", "", true)

                val params = genai.createGeneratorParams()
                params.use { p ->
                    p.setSearchOption("max_length", trimmed.maxTokens.toDouble())
                    p.setSearchOption("temperature", trimmed.temperature)
                    val output = StringBuilder()
                    genai.generate(p, prompt) { chunk ->
                        if (chunk.isNotBlank()) output.append(chunk)
                    }
                    output.toString().trim()
                }
            }
        }
    }

    private fun ensureRuntime(): SimpleGenAI {
        runtime?.let { return it }
        val path = modelManager.modelDirectory().absolutePath
        return SimpleGenAI(path).also { runtime = it }
    }

    private fun ensureTokenizer(genai: SimpleGenAI): Tokenizer {
        tokenizer?.let { return it }
        return Tokenizer(genai.model).also { tokenizer = it }
    }
}
