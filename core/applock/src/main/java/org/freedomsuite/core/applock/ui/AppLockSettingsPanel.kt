package org.freedomsuite.core.applock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.freedomsuite.core.applock.AppLockSettings

@Composable
fun AppLockSettingsPanel(
    settings: AppLockSettings,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "App lock",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Biometric unlock")
                Text(
                    text = "Use fingerprint or face when available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Switch(
                checked = settings.biometricEnabled,
                onCheckedChange = { settings.biometricEnabled = it },
            )
        }
        Text(
            text = "Grace period: ${settings.gracePeriodSeconds}s",
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Re-open within this time without re-entering PIN. Set to 0 to always require unlock.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Slider(
            value = settings.gracePeriodSeconds.toFloat(),
            onValueChange = { settings.gracePeriodSeconds = it.toInt() },
            valueRange = 0f..120f,
            steps = 11,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
