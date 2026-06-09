package org.freedomsuite.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.freedomsuite.calendar.data.CalendarDatabase

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduler = EventReminderScheduler(context.applicationContext)
        runBlocking {
            val events = CalendarDatabase.getInstance(context).calendarDao().getAllEvents()
            scheduler.rescheduleAll(events)
        }
    }
}
