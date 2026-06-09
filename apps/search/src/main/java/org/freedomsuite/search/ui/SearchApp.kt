package org.freedomsuite.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchHit
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchApp(viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsState()
    val grouped by viewModel.groupedResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val installedSources by viewModel.installedSources.collectAsState()
    val totalHits = grouped.values.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("car, meeting, passport…") },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )
                IconButton(
                    onClick = { viewModel.submitSearch() },
                    enabled = query.isNotBlank() && !isSearching,
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }

            Text(
                text = "On-device search across Mail, Photos, Calendar, and Messages. " +
                    "Photo hits use vision labels and OCR already indexed on your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (installedSources.isNotEmpty() && query.isBlank()) {
                Text(
                    text = "Sources: ${installedSources.joinToString { it.label }}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                query.isBlank() -> {
                    EmptyState(
                        title = "Search everything locally",
                        body = "Type a word to find emails, photos (objects & text), events, and messages.",
                    )
                }
                totalHits == 0 -> {
                    EmptyState(
                        title = "No matches",
                        body = "Try synonyms like “vehicle” for car, or index photos in Files first.",
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SearchBridge.Source.entries.forEach { source ->
                            val hits = grouped[source].orEmpty()
                            if (hits.isEmpty()) return@forEach
                            item(key = "header-${source.id}") {
                                SourceSectionHeader(source = source, count = hits.size)
                            }
                            item(key = "tiles-${source.id}") {
                                ResultTileGrid(
                                    hits = hits,
                                    source = source,
                                    onOpen = { viewModel.openHit(it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSectionHeader(source: SearchBridge.Source, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = sourceIcon(source),
            contentDescription = null,
            tint = sourceTint(source),
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = "${source.label} · $count",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultTileGrid(
    hits: List<SearchHit>,
    source: SearchBridge.Source,
    onOpen: (SearchHit) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        hits.forEach { hit ->
            SearchResultTile(
                hit = hit,
                source = source,
                onClick = { onOpen(hit) },
            )
        }
    }
}

@Composable
private fun SearchResultTile(
    hit: SearchHit,
    source: SearchBridge.Source,
    onClick: () -> Unit,
) {
    val tint = sourceTint(source)
    Card(
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(tint.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = sourceIcon(source),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = hit.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (hit.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hit.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hit.snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hit.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hit.timestampMs > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(hit.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}

private fun sourceIcon(source: SearchBridge.Source): ImageVector = when (source) {
    SearchBridge.Source.MAIL -> Icons.Default.Email
    SearchBridge.Source.PHOTO -> Icons.Default.Image
    SearchBridge.Source.CALENDAR -> Icons.Default.CalendarMonth
    SearchBridge.Source.MESSAGE -> Icons.Default.Forum
}

@Composable
private fun sourceTint(source: SearchBridge.Source) = when (source) {
    SearchBridge.Source.MAIL -> MaterialTheme.colorScheme.primary
    SearchBridge.Source.PHOTO -> MaterialTheme.colorScheme.tertiary
    SearchBridge.Source.CALENDAR -> MaterialTheme.colorScheme.secondary
    SearchBridge.Source.MESSAGE -> MaterialTheme.colorScheme.error
}
