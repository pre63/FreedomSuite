package org.freedomsuite.chat.ui

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.freedomsuite.chat.model.ChatMessage
import org.freedomsuite.chat.model.ImageContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val attachedImage by viewModel.attachedImage.collectAsState()

    // Speak assistant responses locally (no network).
    val ttsState = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { }
        ttsState.value = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    var lastSpokenAssistantId by remember { mutableStateOf<String?>(null) }
    val lastAssistant = messages.lastOrNull { it.role == ChatMessage.Role.ASSISTANT }
    if (lastAssistant != null && lastAssistant.id != lastSpokenAssistantId) {
        val text = lastAssistant.content.trim()
        if (text.isNotEmpty()) {
            ttsState.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, lastAssistant.id)
            lastSpokenAssistantId = lastAssistant.id
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Prefer not to exfiltrate image bytes; we only compute a local textual summary.
        viewModel.attachImageFromUri(uri, fileName = "chat_image")
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (messages.isEmpty()) {
                Text(
                    "Start a conversation. You can optionally attach an image (locally described via on-device vision + OCR).",
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }

            attachedImage?.let { img ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Image summary (local)", fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(describe(img), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { viewModel.setAttachedImage(null) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove image")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(onClick = {
                    if (isListening) viewModel.stopVoiceToText() else viewModel.startVoiceToText()
                }) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice input",
                    )
                }
                IconButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Default.Image, contentDescription = "Attach image")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = viewModel::setInputText,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type a message…") },
                minLines = 1,
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = viewModel::sendCurrentMessage,
                    enabled = inputText.isNotBlank() || attachedImage != null,
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.Role.USER
    val bg = if (isUser) {
        androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = if (isUser) {
        androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isUser) "You" else "Assistant",
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = message.content, color = textColor)
        }
    }
}

private fun describe(img: ImageContext): String {
    val labels = img.labels.take(8).joinToString(", ")
    val ocr = img.ocrTextPreview.trim().take(220)
    val faces = if (img.facesCount > 0) "faces=${img.facesCount}" else ""
    return listOf(
        if (labels.isBlank()) null else "objects: $labels",
        if (faces.isBlank()) null else faces,
        if (ocr.isBlank()) null else "ocr: $ocr",
    ).filterNotNull().joinToString("\n")
}

