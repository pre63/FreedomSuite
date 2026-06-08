package org.freedomsuite.core.applock

enum class AppLockState {
    SetupRequired,
    Locked,
    Unlocked,
}

class AppLockManager(
    private val settings: AppLockSettings,
) {
    private var lastUnlockEpochMs: Long = 0L
    private var backgroundedAtEpochMs: Long = 0L

    fun currentState(): AppLockState = when {
        !settings.isPinConfigured -> AppLockState.SetupRequired
        isWithinGracePeriod() -> AppLockState.Unlocked
        lastUnlockEpochMs == 0L -> AppLockState.Locked
        else -> AppLockState.Locked
    }

    fun onUnlocked() {
        lastUnlockEpochMs = System.currentTimeMillis()
    }

    fun onAppBackgrounded() {
        backgroundedAtEpochMs = System.currentTimeMillis()
        if (settings.gracePeriodSeconds == 0) {
            lastUnlockEpochMs = 0L
        }
    }

    fun onAppForegrounded(): AppLockState {
        if (!settings.isPinConfigured) return AppLockState.SetupRequired
        if (isWithinGracePeriod()) return AppLockState.Unlocked
        lastUnlockEpochMs = 0L
        return AppLockState.Locked
    }

    fun lockNow() {
        lastUnlockEpochMs = 0L
    }

    private fun isWithinGracePeriod(): Boolean {
        if (lastUnlockEpochMs == 0L) return false
        val graceMs = settings.gracePeriodSeconds * 1000L
        if (graceMs == 0L) return false
        val reference = if (backgroundedAtEpochMs > 0) backgroundedAtEpochMs else lastUnlockEpochMs
        return System.currentTimeMillis() - reference < graceMs
    }
}
