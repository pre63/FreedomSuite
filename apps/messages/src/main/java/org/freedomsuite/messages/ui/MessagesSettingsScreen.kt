package org.freedomsuite.messages.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.freedomsuite.core.applock.AppLockSettings
import org.freedomsuite.core.applock.ui.AppLockSettingsPanel
import org.freedomsuite.core.network.MailboxOrgDefaults
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesSettingsScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppLockSettings(context) }
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var syncConfig by remember { mutableStateOf(viewModel.loadSyncConfig()) }
    var webDavUrl by remember(syncConfig) { mutableStateOf(syncConfig.webDavUrl ?: MailboxOrgDefaults.WEBDAV_URL) }
    var webDavEmail by remember(syncConfig) { mutableStateOf(syncConfig.webDavEmail.orEmpty()) }
    var webDavPassword by remember(syncConfig) { mutableStateOf(syncConfig.webDavPassword?.concatToString().orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            AppLockSettingsPanel(settings = settings)

            Text(
                text = "Freedom Sync",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                text = "Encrypted backup of your message channels and messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            RowChips(
                selected = syncConfig.backend,
                onSelect = { backend ->
                    syncConfig = syncConfig.copy(backend = backend)
                    viewModel.saveSyncBackend(backend)
                },
            )

            if (syncConfig.backend == SyncBackend.MAILBOX_WEBDAV) {
                OutlinedTextField(
                    value = webDavUrl,
                    onValueChange = { webDavUrl = it },
                    label = { Text("WebDAV URL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = webDavEmail,
                    onValueChange = { webDavEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = webDavPassword,
                    onValueChange = { webDavPassword = it },
                    label = { Text("App password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = {
                        val updated = SyncConfig(
                            backend = SyncBackend.MAILBOX_WEBDAV,
                            webDavUrl = webDavUrl,
                            webDavEmail = webDavEmail,
                            webDavPassword = webDavPassword.toCharArray(),
                        )
                        syncConfig = updated
                        viewModel.saveSyncConfig(updated)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Save WebDAV credentials")
                }
            }

            val lastSync = viewModel.lastSyncEpochMs()
            if (lastSync > 0) {
                val formatted = DateFormat.getDateTimeInstance().format(Date(lastSync))
                Text(
                    text = "Last sync: $formatted",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (syncStatus != null) {
                Text(
                    text = syncStatus!!,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = { viewModel.syncNow() },
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("Sync now")
            }
            Button(
                onClick = { viewModel.restoreFromSync() },
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Restore from sync")
            }
        }
    }
}

@Composable
private fun RowChips(selected: SyncBackend, onSelect: (SyncBackend) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        FilterChip(
            selected = selected == SyncBackend.LOCAL_ONLY,
            onClick = { onSelect(SyncBackend.LOCAL_ONLY) },
            label = { Text("Local only") },
        )
        FilterChip(
            selected = selected == SyncBackend.MAILBOX_WEBDAV,
            onClick = { onSelect(SyncBackend.MAILBOX_WEBDAV) },
            label = { Text("mailbox.org WebDAV") },
            modifier = Modifier.padding(top = 8.dp),
        )
        FilterChip(
            selected = selected == SyncBackend.S3_COMPATIBLE,
            onClick = { onSelect(SyncBackend.S3_COMPATIBLE) },
            label = { Text("S3-compatible") },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
