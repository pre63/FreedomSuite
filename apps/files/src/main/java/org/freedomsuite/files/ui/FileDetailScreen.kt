package org.freedomsuite.files.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.freedomsuite.files.data.FileItemEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    viewModel: FilesViewModel,
    fileId: String,
    onBack: () -> Unit,
    onOpenSimilar: (String) -> Unit = {},
) {
    var file by remember { mutableStateOf<FileItemEntity?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val extractedText by viewModel.extractedText.collectAsState()
    val similarFaces by viewModel.similarFaces.collectAsState()
    val analysisLabels by viewModel.analysisLabels.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(fileId) {
        viewModel.clearExtractedText()
        file = viewModel.getFile(fileId)
        viewModel.loadPhotoAnalysis(fileId)
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete file?") },
            text = { Text(file?.displayName.orEmpty()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(fileId)
                        showDelete = false
                        onBack()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file?.displayName ?: "File") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!extractedText.isNullOrBlank()) {
                        IconButton(onClick = { viewModel.copyExtractedText(context) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy all text")
                        }
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        val current = file
        if (current == null) {
            Text(
                text = "Loading…",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (current.isImage) {
                val bitmap = remember(current.id) {
                    runCatching {
                        val bytes = viewModel.readFileBytes(current)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = current.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                analysisLabels?.let { labels ->
                    Text(
                        text = "Detected: $labels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (similarFaces.isNotEmpty()) {
                    Text(
                        text = "Similar faces",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        similarFaces.forEach { match ->
                            SimilarFaceThumb(
                                file = match,
                                viewModel = viewModel,
                                onClick = { onOpenSimilar(match.id) },
                            )
                        }
                    }
                }
                if (extractedText == null) {
                    Button(
                        onClick = { viewModel.extractTextFromImage(fileId) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Text(if (isLoading) "Extracting text…" else "Extract text from image")
                    }
                    Text(
                        text = "On-device OCR — nothing leaves your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Recognized text",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            SelectionContainer {
                                Text(
                                    text = extractedText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.copyExtractedText(context) },
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Text("Copy all", modifier = Modifier.padding(start = 8.dp))
                            }
                            TextButton(
                                onClick = { viewModel.clearExtractedText() },
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text("Hide text")
                            }
                        }
                    }
                }
            }
            Text(text = current.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = current.mimeType,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "${formatSize(current.sizeBytes)} · ${formatDate(current.createdAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Stored encrypted on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun formatDate(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))

@Composable
private fun SimilarFaceThumb(
    file: FileItemEntity,
    viewModel: FilesViewModel,
    onClick: () -> Unit,
) {
    val bitmap = remember(file.id) {
        runCatching {
            val bytes = viewModel.readFileBytes(file)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
    Card(
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = file.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
