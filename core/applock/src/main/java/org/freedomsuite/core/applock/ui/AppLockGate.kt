package org.freedomsuite.core.applock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.freedomsuite.core.applock.AppLockManager
import org.freedomsuite.core.applock.AppLockSettings
import org.freedomsuite.core.applock.AppLockState

/**
 * Gates app content behind PIN / biometric unlock.
 * Re-locks after the configured grace period when the app returns from background.
 */
@Composable
fun AppLockGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings = remember { AppLockSettings(context) }
    val manager = remember { AppLockManager(settings) }
    var lockState by remember { mutableStateOf(resolveInitialState(manager, settings)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> manager.onAppBackgrounded()
                Lifecycle.Event.ON_START -> {
                    lockState = manager.onAppForegrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (lockState) {
        AppLockState.SetupRequired -> PinSetupScreen(
            settings = settings,
            onSetupComplete = {
                manager.onUnlocked()
                lockState = AppLockState.Unlocked
            },
        )
        AppLockState.Locked -> LockScreen(
            activity = activity,
            settings = settings,
            onUnlocked = {
                manager.onUnlocked()
                lockState = AppLockState.Unlocked
            },
        )
        AppLockState.Unlocked -> content()
    }
}

private fun resolveInitialState(manager: AppLockManager, settings: AppLockSettings): AppLockState {
    if (!settings.isPinConfigured) return AppLockState.SetupRequired
    return AppLockState.Locked
}
