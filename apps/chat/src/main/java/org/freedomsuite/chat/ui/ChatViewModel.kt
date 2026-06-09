package org.freedomsuite.chat.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedomsuite.chat.config.ChatConfigStore
import org.freedomsuite.chat.llm.ChatLlmBridge
import org.freedomsuite.chat.model.ChatMessage
import org.freedomsuite.chat.model.ImageContext
import org.freedomsuite.core.keyboard.speech.LocalSpeechRecognizer
import org.freedomsuite.core.keyboard.speech.StubSpeechRecognizer
import org.freedomsuite.core.vision.LocalVisionEngine
import java.io.ByteArrayOutputStream
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val configStore = ChatConfigStore(appContext)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _attachedImage = MutableStateFlow<ImageContext?>(null)
    val attachedImage: StateFlow<ImageContext?> = _attachedImage.asStateFlow()

    private val speechRecognizer: LocalSpeechRecognizer = StubSpeechRecognizer()

    fun setInputText(value: String) {
        _inputText.value = value
    }

    fun clearConversation() {
        _messages.value = emptyList()
    }

    fun setAttachedImage(imageContext: ImageContext?) {
        _attachedImage.value = imageContext
    }

    fun startVoiceToText() {
        if (_isListening.value) return
        _isListening.value = true

        viewModelScope.launch {
            speechRecognizer.prepare()
            speechRecognizer.startListening().collect { partial ->
                // Stub emits a single final message; later engines can stream partial updates.
                _inputText.value = partial.text
                if (partial.isFinal) {
                    stopVoiceToText()
                }
            }
        }
    }

    fun stopVoiceToText() {
        if (!_isListening.value) return
        _isListening.value = false
        speechRecognizer.stopListening()
    }

    fun sendCurrentMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() && _attachedImage.value == null) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatMessage.Role.USER,
            content = text,
            imageContext = _attachedImage.value,
        )

        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _attachedImage.value = null

        viewModelScope.launch {
            val thinkingId = UUID.randomUUID().toString()
            val thinking = ChatMessage(
                id = thinkingId,
                role = ChatMessage.Role.ASSISTANT,
                content = "Thinking…",
                imageContext = null,
            )
            _messages.value = _messages.value + thinking

            val requestMessages = _messages.value.filter { it.id != thinkingId }
            val llmService = ChatLlmBridge.createService(appContext, configStore)
            val llmRequest = ChatLlmBridge.toLlmRequest(
                messages = requestMessages,
                trailingVision = userMessage.imageContext,
            )
            val response = llmService.complete(llmRequest)
            _messages.value = _messages.value.map { msg ->
                if (msg.id == thinkingId) msg.copy(content = response.text) else msg
            }
        }
    }

    fun attachImageFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val bytes = readBytes(uri)
            val analysis = withContext(Dispatchers.Default) {
                LocalVisionEngine.indexImage(
                    context = appContext,
                    imageBytes = bytes,
                    fileName = fileName,
                    includeOcr = true,
                ).getOrNull()
            }

            val ctx = analysis?.let { result ->
                val labels = result.objects.map { it.label }.distinct()
                ImageContext(
                    labels = labels,
                    facesCount = result.faces.size,
                    ocrTextPreview = result.ocrText,
                )
            }
            _attachedImage.value = ctx
        }
    }

    private suspend fun readBytes(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open image URI: $uri" }
            val out = ByteArrayOutputStream()
            input.copyTo(out)
            out.toByteArray()
        }
    }
}

