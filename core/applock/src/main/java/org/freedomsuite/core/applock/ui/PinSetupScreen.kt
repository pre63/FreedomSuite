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
import org.freedomsuite.core.applock.AppLockSettings

@Composable
fun PinSetupScreen(
    settings: AppLockSettings,
    onSetupComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(SetupStep.Enter) }
    var firstPin by remember { mutableStateOf("") }
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
            title = when (step) {
                SetupStep.Enter -> "Create PIN"
                SetupStep.Confirm -> "Confirm PIN"
            },
            subtitle = "Choose a ${AppLockSettings.PIN_LENGTH}-digit code to protect this app",
            error = error,
            modifier = Modifier.fillMaxSize(),
            onComplete = { entered ->
                when (step) {
                    SetupStep.Enter -> {
                        firstPin = entered
                        pin = ""
                        step = SetupStep.Confirm
                    }
                    SetupStep.Confirm -> {
                        if (entered == firstPin) {
                            settings.savePin(entered)
                            onSetupComplete()
                        } else {
                            error = "PINs do not match"
                            pin = ""
                            step = SetupStep.Enter
                            firstPin = ""
                        }
                    }
                }
            },
        )
    }
}

private enum class SetupStep {
    Enter,
    Confirm,
}
