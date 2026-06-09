package org.freedomsuite.calendar.reminder

object ReminderOptions {
    const val EMAIL_INVITE_DEFAULT_MINUTES = 15

    val choices: List<Int?> = listOf(null, 0, 5, 15, 30, 60, 1440)

    fun label(minutes: Int?): String = when (minutes) {
        null -> "None"
        0 -> "At event time"
        5 -> "5 minutes before"
        15 -> "15 minutes before"
        30 -> "30 minutes before"
        60 -> "1 hour before"
        1440 -> "1 day before"
        else -> "$minutes minutes before"
    }

    fun triggerEpochMs(startEpochMs: Long, reminderMinutes: Int): Long =
        startEpochMs - reminderMinutes * 60_000L
}
