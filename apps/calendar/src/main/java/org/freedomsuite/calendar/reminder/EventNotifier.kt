package org.freedomsuite.calendar.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.freedomsuite.calendar.MainActivity
import org.freedomsuite.calendar.R

object EventNotifier {
    private const val CHANNEL_ID = "freedom_calendar_reminders"

    fun show(context: Context, eventUid: String, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EventReminderScheduler.EXTRA_EVENT_UID, eventUid)
        }
        val pending = PendingIntent.getActivity(
            context,
            eventUid.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText("Starting soon")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(eventUid.hashCode(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Event reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Local reminders for calendar events"
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}
