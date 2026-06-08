package org.freedomsuite.messages

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class MessagesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
