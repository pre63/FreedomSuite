package org.freedomsuite.auth

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class AuthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
