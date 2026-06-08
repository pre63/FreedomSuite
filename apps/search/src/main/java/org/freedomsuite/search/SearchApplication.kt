package org.freedomsuite.search

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class SearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
