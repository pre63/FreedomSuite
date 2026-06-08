package org.freedomsuite.auth.ui

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
fun AuthSettingsScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppLockSettings(context) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val status by viewModel.status.collectAsState()

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
                text = "Encrypted backup",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(
                text = "Like Authy — back up your TOTP secrets with a passphrase you choose. " +
                    "Restore on a new device using the same passphrase and sync destination. " +
                    "Only ciphertext leaves your device.",
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

            Text(
                text = "Backup passphrase",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = backupPassphrase,
                onValueChange = { backupPassphrase = it },
                label = { Text("New backup passphrase") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            OutlinedTextField(
                value = confirmPassphrase,
                onValueChange = { confirmPassphrase = it },
                label = { Text("Confirm passphrase") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            val lastBackup = viewModel.lastBackupEpochMs()
            if (lastBackup > 0) {
                Text(
                    text = "Last backup: ${DateFormat.getDateTimeInstance().format(Date(lastBackup))}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            if (status != null) {
                Text(text = status!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    if (backupPassphrase.length >= 8 && backupPassphrase == confirmPassphrase) {
                        viewModel.backupNow(backupPassphrase.toCharArray())
                    }
                },
                enabled = !isLoading && backupPassphrase.length >= 8 && backupPassphrase == confirmPassphrase,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("Back up now")
            }

            Text(
                text = "Restore on new device",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = restorePassphrase,
                onValueChange = { restorePassphrase = it },
                label = { Text("Backup passphrase") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = { viewModel.restoreFromBackup(restorePassphrase.toCharArray()) },
                enabled = !isLoading && restorePassphrase.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Restore from backup")
            }
        }
    }
}

@Composable
private fun BackupBackendChips(selected: SyncBackend, onSelect: (SyncBackend) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        FilterChip(
            selected = selected == SyncBackend.LOCAL_ONLY,
            onClick = { onSelect(SyncBackend.LOCAL_ONLY) },
            label = { Text("Local device backup") },
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
