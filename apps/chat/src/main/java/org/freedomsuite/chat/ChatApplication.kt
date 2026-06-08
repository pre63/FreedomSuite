package org.freedomsuite.chat

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
