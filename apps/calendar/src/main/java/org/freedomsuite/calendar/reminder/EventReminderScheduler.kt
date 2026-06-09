package org.freedomsuite.calendar.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.freedomsuite.calendar.data.EventEntity

class EventReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(event: EventEntity) {
        cancel(event.uid)
        val minutes = event.reminderMinutesBefore ?: return
        val triggerAt = ReminderOptions.triggerEpochMs(event.startEpochMs, minutes)
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(appContext, EventReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_UID, event.uid)
            putExtra(EXTRA_EVENT_TITLE, event.title)
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode(event.uid),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(eventUid: String) {
        val intent = Intent(appContext, EventReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode(eventUid),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    fun rescheduleAll(events: List<EventEntity>) {
        events.forEach { schedule(it) }
    }

    private fun requestCode(uid: String): Int = uid.hashCode()

    companion object {
        const val EXTRA_EVENT_UID = "event_uid"
        const val EXTRA_EVENT_TITLE = "event_title"
    }
}
