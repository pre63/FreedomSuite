package org.freedomsuite.keyboard.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.freedomsuite.core.keyboard.SuggestionEngine

@Composable
fun KeyboardScreen(
    partialWord: String,
    suggestions: SuggestionEngine.Suggestions,
    shiftEnabled: Boolean,
    isListening: Boolean,
    onSuggestion: (String) -> Unit,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onMic: () -> Unit,
) {
    val bar = buildList {
        addAll(suggestions.completions)
        addAll(suggestions.corrections.filter { it !in this })
        addAll(suggestions.nextWords.filter { it !in this })
    }.take(3)

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            SuggestionBar(bar, onSuggestion)
            KeyboardRows(
                partialWord = partialWord,
                shiftEnabled = shiftEnabled,
                isListening = isListening,
                onKey = onKey,
                onDelete = onDelete,
                onSpace = onSpace,
                onEnter = onEnter,
                onMic = onMic,
            )
        }
    }
}

@Composable
private fun SuggestionBar(items: List<String>, onSuggestion: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (items.isEmpty()) {
            Text("Keyboard", modifier = Modifier.padding(8.dp))
        } else {
            items.forEach { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .clickable { onSuggestion(item) }
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun KeyboardRows(
    partialWord: String,
    shiftEnabled: Boolean,
    isListening: Boolean,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onMic: () -> Unit,
) {
    val rows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    )
    Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    KeyButton(key, onClick = { onKey(key) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(if (isListening) "Stop" else "Mic", onClick = onMic)
            KeyButton("Space", onClick = onSpace, wide = true)
            KeyButton("Del", onClick = onDelete)
            KeyButton("Enter", onClick = onEnter)
        }
        if (partialWord.isNotEmpty()) {
            Text("Typing: $partialWord", modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit, wide: Boolean = false) {
    Text(
        text = label,
        modifier = Modifier
            .then(if (wide) Modifier.fillMaxWidth(0.5f) else Modifier)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    )
}
