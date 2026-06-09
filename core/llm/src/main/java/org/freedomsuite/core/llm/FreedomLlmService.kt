package org.freedomsuite.core.llm

import android.content.Context

data class RemoteLlmConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val chatPath: String = "/v1/chat/completions",
)

/**
 * Suite LLM facade. On-device inference is always wired; remote is optional.
 */
class FreedomLlmService(
    private val onDevice: OnDeviceLlmBackend,
    private val remote: LlmBackend?,
    private val routing: LlmRouting,
) {
    val onDeviceModelState: LlmModelState
        get() = onDevice.modelState()

    fun onDeviceStatus(): String =
        when (onDeviceModelState) {
            LlmModelState.READY -> "On-device model ready"
            LlmModelState.NOT_INSTALLED ->
                "On-device model not installed — run: make fetch-llm && make push-llm-model"
            LlmModelState.RUNTIME_UNAVAILABLE ->
                "GenAI runtime missing — run: make fetch-llm"
        }

    suspend fun complete(request: LlmRequest): LlmResponse {
        return when (routing) {
            LlmRouting.ON_DEVICE_ONLY -> onDevice.complete(request)
            LlmRouting.REMOTE_ONLY -> {
                val remoteBackend = remote
                    ?: return onDevice.complete(request)
                remoteBackend.complete(request)
            }
            LlmRouting.REMOTE_WITH_ON_DEVICE_FALLBACK -> {
                val remoteBackend = remote
                if (remoteBackend != null && remoteBackend.isAvailable()) {
                    val remoteResponse = remoteBackend.complete(request)
                    if (!remoteResponse.isError) return remoteResponse
                }
                onDevice.complete(request)
            }
        }
    }

    companion object {
        fun create(
            context: Context,
            routing: LlmRouting,
            remoteConfig: RemoteLlmConfig? = null,
        ): FreedomLlmService {
            val onDevice = OnDeviceLlmBackend.create(context)
            val remote = remoteConfig?.takeIf {
                it.baseUrl.isNotBlank() && it.apiKey.isNotBlank()
            }?.let {
                RemoteOpenAiCompatibleBackend(
                    baseUrl = it.baseUrl,
                    apiKey = it.apiKey,
                    model = it.model,
                    chatPath = it.chatPath,
                )
            }
            return FreedomLlmService(
                onDevice = onDevice,
                remote = remote,
                routing = routing,
            )
        }
    }
}
