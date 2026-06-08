package org.freedomsuite.core.logging

import android.app.Application
import org.freedomsuite.core.logging.BuildConfig

/**
 * Initializes privacy settings for all Freedom Suite apps.
 * Call from [Application.onCreate].
 */
object PrivacyInitializer {
    fun init(application: Application) {
        if (!BuildConfig.PRIVACY_STRICT) {
            plantTimberDebugTree()
        }

        Thread.setDefaultUncaughtExceptionHandler(
            FreedomUncaughtExceptionHandler(
                previous = Thread.getDefaultUncaughtExceptionHandler(),
                strict = BuildConfig.PRIVACY_STRICT,
            )
        )
    }

    private fun plantTimberDebugTree() {
        try {
            val timber = Class.forName("timber.log.Timber")
            val debugTree = Class.forName("timber.log.Timber\$DebugTree")
                .getDeclaredConstructor()
                .newInstance()
            timber.getMethod("plant", Class.forName("timber.log.Timber\$Tree"))
                .invoke(null, debugTree)
        } catch (_: ClassNotFoundException) {
            // Dev flavor without Timber — ignore
        }
    }
}
