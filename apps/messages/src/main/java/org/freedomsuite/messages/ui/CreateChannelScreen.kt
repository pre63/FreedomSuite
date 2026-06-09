package org.freedomsuite.messages.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.freedomsuite.messages.data.ChannelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val type = ChannelType.PERSONAL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Channel name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                text = "Encrypted notes channel — private to this device. Sync via Freedom Sync in Settings.",
                modifier = Modifier.padding(top = 16.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createChannel(name, type, onCreated)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                enabled = name.isNotBlank(),
            ) {
                Text("Create channel")
            }
        }
    }
}
