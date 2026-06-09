package org.freedomsuite.chat.config

import android.content.Context
import org.freedomsuite.core.crypto.SecurePreferences

enum class ChatProvider {
    LOCAL,
    GROK,
}

/**
 * Stores chat backend configuration.
 *
 * Security: values are stored in encrypted SharedPreferences.
 */
class ChatConfigStore(context: Context) {
    private val prefs = SecurePreferences(context, "freedom_chat_prefs")

    fun getProvider(): ChatProvider =
        when (prefs.getString(KEY_PROVIDER)) {
            ChatProvider.GROK.name -> ChatProvider.GROK
            else -> ChatProvider.LOCAL
        }

    fun setProvider(provider: ChatProvider) {
        prefs.putString(KEY_PROVIDER, provider.name)
    }

    fun getGrokBaseUrl(): String? = prefs.getString(KEY_GROK_BASE_URL)
    fun setGrokBaseUrl(value: String?) = setMaybeEmptyString(KEY_GROK_BASE_URL, value)

    fun getGrokApiKey(): String? = prefs.getString(KEY_GROK_API_KEY)
    fun setGrokApiKey(value: String?) = setMaybeEmptyString(KEY_GROK_API_KEY, value)

    fun getGrokModel(): String = prefs.getString(KEY_GROK_MODEL) ?: "grok-2"
    fun setGrokModel(value: String) {
        prefs.putString(KEY_GROK_MODEL, value)
    }

    /**
     * Path relative to base URL where we post chat-completions.
     * Example: baseUrl="https://api.x.ai" => url="https://api.x.ai/v1/chat/completions"
     */
    fun getGrokChatPath(): String = prefs.getString(KEY_GROK_CHAT_PATH) ?: "/v1/chat/completions"
    fun setGrokChatPath(value: String) {
        prefs.putString(KEY_GROK_CHAT_PATH, value)
    }

    private fun setMaybeEmptyString(key: String, value: String?) {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) prefs.remove(key) else prefs.putString(key, normalized)
    }

    companion object {
        private const val KEY_PROVIDER = "provider"
        private const val KEY_GROK_BASE_URL = "grok_base_url"
        private const val KEY_GROK_API_KEY = "grok_api_key"
        private const val KEY_GROK_MODEL = "grok_model"
        private const val KEY_GROK_CHAT_PATH = "grok_chat_path"
    }
}

