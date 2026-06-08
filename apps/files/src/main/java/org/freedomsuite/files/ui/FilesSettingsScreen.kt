package org.freedomsuite.files.ui

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
fun FilesSettingsScreen(
    viewModel: FilesViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppLockSettings(context) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var syncConfig by remember { mutableStateOf(viewModel.loadSyncConfig()) }
    var backupPassphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
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
                text = "Photo intelligence",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                text = "Index photos on-device for face matching and word search (car, person, id card, etc.). " +
                    "No cloud. New imports are indexed automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(
                onClick = { viewModel.indexAllPhotos() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Index all photos")
            }

            Text(
                text = "Encrypted backup",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                text = "Back up all folders and files with a passphrase you choose. " +
                    "File contents are included in the backup. Only ciphertext leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            BackupBackendChips(
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = webDavEmail,
                    onValueChange = { webDavEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = webDavPassword,
                    onValueChange = { webDavPassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        viewModel.saveSyncConfig(
                            syncConfig.copy(
                                webDavUrl = webDavUrl,
                                webDavEmail = webDavEmail,
                                webDavPassword = webDavPassword.toCharArray(),
                            ),
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Save WebDAV settings") }
            }

            OutlinedTextField(
                value = backupPassphrase,
                onValueChange = { backupPassphrase = it },
                label = { Text("Backup passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlinedTextField(
                value = confirmPassphrase,
                onValueChange = { confirmPassphrase = it },
                label = { Text("Confirm passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = {
                    if (backupPassphrase == confirmPassphrase && backupPassphrase.isNotBlank()) {
                        viewModel.backupNow(backupPassphrase.toCharArray())
                    }
                },
                enabled = !isLoading && backupPassphrase == confirmPassphrase && backupPassphrase.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                if (isLoading) CircularProgressIndicator() else Text("Back up now")
            }

            val lastBackup = viewModel.lastBackupEpochMs()
            if (lastBackup > 0) {
                Text(
                    text = "Last backup: ${DateFormat.getDateTimeInstance().format(Date(lastBackup))}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            OutlinedTextField(
                value = restorePassphrase,
                onValueChange = { restorePassphrase = it },
                label = { Text("Restore passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            Button(
                onClick = {
                    if (restorePassphrase.isNotBlank()) {
                        viewModel.restoreBackup(restorePassphrase.toCharArray())
                    }
                },
                enabled = !isLoading && restorePassphrase.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Restore from backup") }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BackupBackendChips(selected: SyncBackend, onSelect: (SyncBackend) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        listOf(
            SyncBackend.LOCAL_ONLY to "Local only",
            SyncBackend.MAILBOX_WEBDAV to "mailbox.org WebDAV",
            SyncBackend.S3_COMPATIBLE to "S3-compatible",
        ).forEach { (backend, label) ->
            FilterChip(
                selected = selected == backend,
                onClick = { onSelect(backend) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        }
    }
}
