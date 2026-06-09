package org.freedomsuite.calendar.ui

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    viewModel: CalendarViewModel,
    uid: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endEpochMs by remember { mutableLongStateOf(System.currentTimeMillis() + 3_600_000) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val event by viewModel.activeEvent.collectAsState()

    LaunchedEffect(uid) {
        viewModel.openEvent(uid)
    }
    LaunchedEffect(event) {
        event?.let {
            title = it.title
            description = it.description.orEmpty()
            location = it.location.orEmpty()
            startEpochMs = it.startEpochMs
            endEpochMs = it.endEpochMs
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit event") },
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
            if (error != null) {
                Text(text = error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            EventFormFields(
                title = title,
                onTitleChange = { title = it },
                location = location,
                onLocationChange = { location = it },
                description = description,
                onDescriptionChange = { description = it },
                startEpochMs = startEpochMs,
                onStartChange = { startEpochMs = it },
                endEpochMs = endEpochMs,
                onEndChange = { endEpochMs = it },
            )
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.updateEvent(
                            uid = uid,
                            title = title,
                            description = description.ifBlank { null },
                            location = location.ifBlank { null },
                            startEpochMs = startEpochMs,
                            endEpochMs = endEpochMs.coerceAtLeast(startEpochMs + 60_000),
                            onSaved = onSaved,
                        )
                    }
                },
                enabled = !isLoading && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Save changes")
            }
        }
    }
}
