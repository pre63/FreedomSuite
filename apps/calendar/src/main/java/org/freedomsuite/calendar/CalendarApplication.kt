package org.freedomsuite.calendar

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.freedomsuite.calendar.data.CalendarRepository
import org.freedomsuite.core.logging.PrivacyInitializer

class CalendarApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PrivacyInitializer.init(this)
        appScope.launch {
            CalendarRepository(this@CalendarApplication).rescheduleAllReminders()
        }
    }
}
