package org.freedomsuite.keyboard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.freedomsuite.core.ui.FreedomTheme
import org.freedomsuite.keyboard.R
import org.freedomsuite.keyboard.data.KeyboardRepository

class KeyboardSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreedomTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { KeyboardRepository(context) }
    var learnedWords by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        learnedWords = repository.learnedWordCount()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_subtitle), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.privacy_note))
            Text(stringResource(R.string.suggestions_learned, learnedWords))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }) {
                Text(stringResource(R.string.enable_keyboard))
            }
            Button(onClick = {
                val imm = context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                imm?.showInputMethodPicker()
            }) {
                Text(stringResource(R.string.choose_keyboard))
            }
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            }) {
                Text(stringResource(R.string.enable_voice))
            }
            Text(stringResource(R.string.voice_model_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.voice_model_body))
        }
    }
}
