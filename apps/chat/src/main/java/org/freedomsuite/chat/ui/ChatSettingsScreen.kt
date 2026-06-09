package org.freedomsuite.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.freedomsuite.chat.config.ChatConfigStore
import org.freedomsuite.chat.config.ChatProvider
import org.freedomsuite.chat.llm.ChatLlmBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val store = remember { ChatConfigStore(ctx.applicationContext) }

    var provider by remember { mutableStateOf(store.getProvider()) }
    var baseUrl by remember { mutableStateOf(store.getGrokBaseUrl().orEmpty()) }
    var apiKey by remember { mutableStateOf(store.getGrokApiKey().orEmpty()) }
    var model by remember { mutableStateOf(store.getGrokModel()) }
    var chatPath by remember { mutableStateOf(store.getGrokChatPath()) }

    var statusText by remember { mutableStateOf<String?>(null) }
    val onDeviceStatus = remember(ctx) {
        ChatLlmBridge.createService(ctx.applicationContext, store).onDeviceStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                "Backend",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                onDeviceStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(10.dp))

            BackendOption(
                label = "On-device LLM (always available)",
                selected = provider == ChatProvider.LOCAL,
                onClick = { provider = ChatProvider.LOCAL },
            )
            BackendOption(
                label = "Remote (Grok / OpenAI-compatible) + on-device fallback",
                selected = provider == ChatProvider.GROK,
                onClick = { provider = ChatProvider.GROK },
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (provider == ChatProvider.GROK) {
                Text(
                    "Grok connection",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (e.g. https://api.x.ai)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key (stored encrypted)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chatPath,
                    onValueChange = { chatPath = it },
                    label = { Text("Chat path") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Privacy: vision runs on-device (ONNX). Remote mode sends text only — never raw images. " +
                    "On-device LLM stays available for offline/fallback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    store.setProvider(provider)
                    if (provider == ChatProvider.GROK) {
                        store.setGrokBaseUrl(baseUrl)
                        store.setGrokApiKey(apiKey)
                        store.setGrokModel(model)
                        store.setGrokChatPath(chatPath)
                    } else {
                        store.setGrokBaseUrl(null)
                        store.setGrokApiKey(null)
                    }
                    statusText = "Saved."
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            statusText?.let { txt ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(txt)
            }
        }
    }
}

@Composable
private fun BackendOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

