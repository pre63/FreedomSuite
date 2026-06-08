package org.freedomsuite.inbox

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class InboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
