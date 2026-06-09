package org.freedomsuite.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendars")
data class CalendarEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val isVisible: Boolean = true,
)

enum class EventSource {
    LOCAL,
    EMAIL,
}

enum class EventResponseStatus {
    NONE,
    NEEDS_ACTION,
    ACCEPTED,
    TENTATIVE,
    DECLINED,
}

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val uid: String,
    val calendarId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val source: String,
    val responseStatus: String,
    val organizer: String?,
    val sourceMailUid: Long?,
    val rawInviteIcs: String?,
    val isAllDay: Boolean = false,
    /** Minutes before start to fire a local notification; null = no reminder. */
    val reminderMinutesBefore: Int? = null,
)
