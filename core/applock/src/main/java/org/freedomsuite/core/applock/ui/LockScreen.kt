package org.freedomsuite.core.applock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import org.freedomsuite.core.applock.AppLockSettings
import org.freedomsuite.core.applock.BiometricHelper

@Composable
fun LockScreen(
    activity: FragmentActivity,
    settings: AppLockSettings,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        PinPad(
            pin = pin,
            onPinChange = {
                pin = it
                error = null
            },
            title = "Enter PIN",
            subtitle = "Freedom Suite is locked",
            error = error,
            modifier = Modifier.fillMaxSize(),
            onComplete = { entered ->
                if (settings.verifyPin(entered)) {
                    pin = ""
                    onUnlocked()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            },
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (settings.biometricEnabled && BiometricHelper.canAuthenticate(activity)) {
            BiometricHelper.show(
                activity = activity,
                onSuccess = onUnlocked,
                onError = { /* fall back to PIN */ },
            )
        }
    }
}
