package org.freedomsuite.inbox.ui

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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSettingsScreen(
    viewModel: InboxViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val accountInfo by viewModel.accountInfo.collectAsState()
    var aliasesText by remember(accountInfo) {
        mutableStateOf(accountInfo?.aliases?.joinToString(", ").orEmpty())
    }
    var domainsText by remember(accountInfo) {
        mutableStateOf(accountInfo?.ownedDomains?.joinToString(", ").orEmpty())
    }

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
                .padding(padding)
                .padding(16.dp),
        ) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Text(
                text = accountInfo?.email ?: "Not connected",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = "Aliases and owned domains help the spam filter recognize mail addressed to you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            OutlinedTextField(
                value = aliasesText,
                onValueChange = { aliasesText = it },
                label = { Text("Aliases (comma-separated)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = { Text("shop@mybrand.com, news@other.org") },
            )
            OutlinedTextField(
                value = domainsText,
                onValueChange = { domainsText = it },
                label = { Text("Owned domains") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("mybrand.com, other.org") },
            )
            Button(
                onClick = {
                    viewModel.saveMailboxExtras(
                        aliases = aliasesText.split(',').map { it.trim() }.filter { it.contains('@') },
                        ownedDomains = domainsText.split(',').map { it.trim().removePrefix("@") }.filter { it.isNotEmpty() },
                    )
                },
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Save aliases")
            }
            Button(
                onClick = {
                    viewModel.signOut()
                    onSignedOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text("Sign out")
            }
        }
    }
}
