package org.freedomsuite.keyboard

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class KeyboardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
