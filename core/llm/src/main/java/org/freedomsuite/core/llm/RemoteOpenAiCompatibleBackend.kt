package org.freedomsuite.core.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.freedomsuite.core.network.PrivacyHttpClient
import org.json.JSONObject

/**
 * User-configured OpenAI-compatible chat API (Grok, OpenRouter, local HTTPS proxy, etc.).
 * Sends **text only** — vision stays as VisionContext in the user message.
 */
class RemoteOpenAiCompatibleBackend(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val chatPath: String = "/v1/chat/completions",
    private val httpClient: OkHttpClient = PrivacyHttpClient.create(),
) : LlmBackend {
    override val id: String = "remote_openai_compat"
    override val displayName: String = "Remote (OpenAI-compatible)"

    override suspend fun isAvailable(): Boolean =
        baseUrl.isNotBlank() && apiKey.isNotBlank()

    override suspend fun complete(request: LlmRequest): LlmResponse = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            return@withContext LlmResponse(
                text = "Remote LLM not configured (base URL and API key required).",
                source = LlmSource.REMOTE,
                isError = true,
            )
        }

        val url = "${baseUrl.trimEnd('/')}/${chatPath.trimStart('/')}"
        val messagesPayload = buildMessagesPayload(request)
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesPayload)
            put("temperature", request.temperature)
            put("stream", false)
        }

        val httpBody = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(httpBody)
            .build()

        runCatching {
            httpClient.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext LlmResponse(
                        text = "Remote request failed (${response.code}).",
                        source = LlmSource.REMOTE,
                        isError = true,
                    )
                }
                val parsed = runCatching { JSONObject(raw) }.getOrNull()
                val content = parsed
                    ?.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                LlmResponse(
                    text = content.ifBlank { raw.take(600) },
                    source = LlmSource.REMOTE,
                )
            }
        }.getOrElse { error ->
            LlmResponse(
                text = "Remote request error: ${error.message}",
                source = LlmSource.REMOTE,
                isError = true,
            )
        }
    }

    private fun buildMessagesPayload(request: LlmRequest): org.json.JSONArray {
        val trimmed = LlmPromptFormatter.trimHistory(request.messages)
        val lastUserIndex = trimmed.indexOfLast { it.role == LlmRole.USER }
        val trailingVision = request.visionContext
        val array = org.json.JSONArray()

        if (request.systemPrompt.isNotBlank()) {
            array.put(
                JSONObject().apply {
                    put("role", LlmRole.SYSTEM.wireName)
                    put("content", request.systemPrompt)
                },
            )
        }

        trimmed.forEachIndexed { index, message ->
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
        return array
    }
}
