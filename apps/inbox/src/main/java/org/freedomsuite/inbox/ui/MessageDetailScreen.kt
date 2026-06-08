package org.freedomsuite.inbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.freedomsuite.inbox.data.InviteStatus
import org.freedomsuite.protocol.ical.InviteResponseStatus
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    viewModel: InboxViewModel,
    uid: Long,
    onBack: () -> Unit,
) {
    LaunchedEffect(uid) {
        viewModel.openMessage(uid)
    }

    val message by viewModel.activeMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val calendarInstalled by viewModel.calendarInstalled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(message?.subject ?: "Message") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.closeMessage()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (message == null && isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(24.dp))
        } else if (message != null) {
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(message!!.dateEpochMs))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(text = message!!.from, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                if (message!!.hasCalendarInvite) {
                    InviteCard(
                        message = message!!,
                        calendarInstalled = calendarInstalled,
                        isLoading = isLoading,
                        onAccept = { viewModel.respondToInvite(InviteResponseStatus.ACCEPTED) },
                        onMaybe = { viewModel.respondToInvite(InviteResponseStatus.TENTATIVE) },
                        onDecline = { viewModel.respondToInvite(InviteResponseStatus.DECLINED) },
                    )
                }
                Text(
                    text = message!!.body.ifBlank { message!!.snippet },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun InviteCard(
    message: org.freedomsuite.inbox.data.MailMessageEntity,
    calendarInstalled: Boolean,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onMaybe: () -> Unit,
    onDecline: () -> Unit,
) {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    val start = message.inviteStartEpochMs?.let { formatter.format(Date(it)) }
    val end = message.inviteEndEpochMs?.let { formatter.format(Date(it)) }
    val responded = message.inviteStatus != InviteStatus.PENDING.name &&
        message.inviteStatus != InviteStatus.NONE.name

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calendar invite", style = MaterialTheme.typography.titleSmall)
            Text(
                text = message.inviteTitle ?: message.subject,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (start != null && end != null) {
                Text(
                    text = "$start → $end",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            message.inviteOrganizer?.let {
                Text(
                    text = "From $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            if (responded) {
                Text(
                    text = "You responded: ${message.inviteStatus.lowercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                if (!calendarInstalled) {
                    Text(
                        text = "Install Freedom Calendar to add accepted events locally.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Accept")
                    }
                    OutlinedButton(
                        onClick = onMaybe,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Maybe")
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}
