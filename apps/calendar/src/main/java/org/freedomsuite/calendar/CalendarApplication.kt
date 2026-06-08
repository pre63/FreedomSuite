package org.freedomsuite.calendar

import android.app.Application
import org.freedomsuite.core.logging.PrivacyInitializer

class CalendarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
    }
}
