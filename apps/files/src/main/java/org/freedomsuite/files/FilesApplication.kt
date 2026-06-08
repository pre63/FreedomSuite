package org.freedomsuite.files

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class FilesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
