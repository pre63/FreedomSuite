package org.freedomsuite.messages.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.freedomsuite.messages.data.ChannelType
import org.freedomsuite.messages.data.ChannelWithPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    viewModel: MessagesViewModel,
    onChannelClick: (String) -> Unit,
    onCreateChannel: () -> Unit,
    onSettings: () -> Unit,
) {
    val channels by viewModel.channels.collectAsState()
    var channelToDelete by remember { mutableStateOf<ChannelWithPreview?>(null) }

    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("Delete channel?") },
            text = { Text("“${channelToDelete!!.channel.name}” and its messages will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChannel(channelToDelete!!.channel.id)
                        channelToDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateChannel) {
                Icon(Icons.Default.Add, contentDescription = "New channel")
            }
        },
    ) { padding ->
        if (channels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Text("No notes yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Create an encrypted channel for private notes and photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(channels, key = { it.channel.id }) { item ->
                    ChannelRow(
                        item = item,
                        onClick = { onChannelClick(item.channel.id) },
                        onDelete = { channelToDelete = item },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    item: ChannelWithPreview,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val channel = item.channel
    val type = runCatching { ChannelType.valueOf(channel.type) }.getOrDefault(ChannelType.PERSONAL)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (type) {
                    ChannelType.PERSONAL -> Icons.Default.Person
                    ChannelType.GROUP -> Icons.Default.Group
                },
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = channel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (type) {
                        ChannelType.PERSONAL -> "Encrypted notes · this device"
                        ChannelType.GROUP -> "Encrypted notes · this device"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.latestPreview?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete channel")
            }
        }
    }
}
