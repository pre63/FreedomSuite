package org.freedomsuite.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uid = intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_UID) ?: return
        val title = intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_TITLE) ?: "Event"
        EventNotifier.show(context.applicationContext, uid, title)
    }
}
