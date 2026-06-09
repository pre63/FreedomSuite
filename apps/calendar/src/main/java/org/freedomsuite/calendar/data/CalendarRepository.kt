package org.freedomsuite.calendar.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.freedomsuite.calendar.reminder.EventReminderScheduler
import org.freedomsuite.calendar.reminder.ReminderOptions
import org.freedomsuite.calendar.ui.AgendaGrouper
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CalendarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = CalendarDatabase.getInstance(appContext).calendarDao()
    private val reminders = EventReminderScheduler(appContext)

    fun observeEvents(): Flow<List<EventEntity>> = dao.observeEvents()

    fun observeCalendars(): Flow<List<CalendarEntity>> = dao.observeCalendars()

    suspend fun ensureDefaultCalendar() {
        if (dao.getDefaultCalendar() == null) {
            dao.upsertCalendar(
                CalendarEntity(
                    id = DEFAULT_CALENDAR_ID,
                    displayName = "Personal",
                ),
            )
        }
    }

    suspend fun getEvent(uid: String): EventEntity? = dao.getEvent(uid)

    suspend fun defaultCalendarId(): String {
        ensureDefaultCalendar()
        return dao.getDefaultCalendar()?.id ?: DEFAULT_CALENDAR_ID
    }

    suspend fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        isAllDay: Boolean = false,
        reminderMinutesBefore: Int? = null,
    ): Result<EventEntity> = runCatching {
        ensureDefaultCalendar()
        val (start, end) = if (isAllDay) {
            AgendaGrouper.normalizeAllDayRange(startEpochMs, endEpochMs)
        } else {
            startEpochMs to endEpochMs
        }
        val entity = EventEntity(
            uid = UUID.randomUUID().toString(),
            calendarId = defaultCalendarId(),
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            startEpochMs = start,
            endEpochMs = end,
            source = EventSource.LOCAL.name,
            responseStatus = EventResponseStatus.NONE.name,
            organizer = null,
            sourceMailUid = null,
            rawInviteIcs = null,
            isAllDay = isAllDay,
            reminderMinutesBefore = reminderMinutesBefore,
        )
        dao.upsertEvent(entity)
        reminders.schedule(entity)
        entity
    }

    suspend fun importEmailEvent(
        uid: String,
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        responseStatus: String,
        organizer: String?,
        sourceMailUid: Long,
        rawInviteIcs: String,
    ): Result<Unit> = runCatching {
        ensureDefaultCalendar()
        val entity = EventEntity(
            uid = uid,
            calendarId = defaultCalendarId(),
            title = title,
            description = description,
            location = location,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            source = EventSource.EMAIL.name,
            responseStatus = responseStatus,
            organizer = organizer,
            sourceMailUid = sourceMailUid,
            rawInviteIcs = rawInviteIcs,
            reminderMinutesBefore = ReminderOptions.EMAIL_INVITE_DEFAULT_MINUTES,
        )
        dao.upsertEvent(entity)
        reminders.schedule(entity)
    }

    suspend fun updateEvent(
        uid: String,
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        isAllDay: Boolean = false,
        reminderMinutesBefore: Int? = null,
    ): Result<EventEntity> = runCatching {
        val existing = dao.getEvent(uid) ?: error("Event not found")
        val (start, end) = if (isAllDay) {
            AgendaGrouper.normalizeAllDayRange(startEpochMs, endEpochMs)
        } else {
            startEpochMs to endEpochMs
        }
        val updated = existing.copy(
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            startEpochMs = start,
            endEpochMs = end,
            isAllDay = isAllDay,
            reminderMinutesBefore = reminderMinutesBefore,
        )
        dao.upsertEvent(updated)
        reminders.schedule(updated)
        updated
    }

    suspend fun deleteEvent(uid: String): Result<Unit> = runCatching {
        reminders.cancel(uid)
        dao.deleteEvent(uid)
    }

    suspend fun rescheduleAllReminders() {
        reminders.rescheduleAll(dao.getAllEvents())
    }

    suspend fun syncBackup(): Result<Unit> = runCatching {
        val engine = buildSyncEngine()
        val snapshot = exportSnapshot().toByteArray(Charsets.UTF_8)
        engine.syncNow(snapshot).getOrThrow()
    }

    suspend fun exportSnapshot(): String {
        ensureDefaultCalendar()
        val root = JSONObject()
        root.put("version", 2)
        val calendars = JSONArray()
        dao.getAllCalendars().forEach { calendar ->
            calendars.put(
                JSONObject()
                    .put("id", calendar.id)
                    .put("displayName", calendar.displayName)
                    .put("isVisible", calendar.isVisible),
            )
        }
        root.put("calendars", calendars)
        val events = JSONArray()
        dao.getAllEvents().forEach { event ->
            events.put(
                JSONObject()
                    .put("uid", event.uid)
                    .put("calendarId", event.calendarId)
                    .put("title", event.title)
                    .put("description", event.description)
                    .put("location", event.location)
                    .put("startEpochMs", event.startEpochMs)
                    .put("endEpochMs", event.endEpochMs)
                    .put("source", event.source)
                    .put("responseStatus", event.responseStatus)
                    .put("organizer", event.organizer)
                    .put("sourceMailUid", event.sourceMailUid)
                    .put("rawInviteIcs", event.rawInviteIcs)
                    .put("isAllDay", event.isAllDay)
                    .put("reminderMinutesBefore", event.reminderMinutesBefore ?: JSONObject.NULL),
            )
        }
        root.put("events", events)
        return root.toString()
    }

    suspend fun importSnapshot(json: String) {
        val root = JSONObject(json)
        val calendars = root.getJSONArray("calendars")
        for (i in 0 until calendars.length()) {
            val item = calendars.getJSONObject(i)
            dao.upsertCalendar(
                CalendarEntity(
                    id = item.getString("id"),
                    displayName = item.getString("displayName"),
                    isVisible = item.optBoolean("isVisible", true),
                ),
            )
        }
        val events = root.getJSONArray("events")
        for (i in 0 until events.length()) {
            val item = events.getJSONObject(i)
            val reminder = if (item.isNull("reminderMinutesBefore")) {
                null
            } else {
                item.optInt("reminderMinutesBefore", ReminderOptions.EMAIL_INVITE_DEFAULT_MINUTES)
            }
            val entity = EventEntity(
                uid = item.getString("uid"),
                calendarId = item.getString("calendarId"),
                title = item.getString("title"),
                description = item.optString("description").takeIf { it.isNotBlank() },
                location = item.optString("location").takeIf { it.isNotBlank() },
                startEpochMs = item.getLong("startEpochMs"),
                endEpochMs = item.getLong("endEpochMs"),
                source = item.optString("source", EventSource.LOCAL.name),
                responseStatus = item.optString("responseStatus", EventResponseStatus.NONE.name),
                organizer = item.optString("organizer").takeIf { it.isNotBlank() },
                sourceMailUid = item.optLong("sourceMailUid").takeIf { it > 0 },
                rawInviteIcs = item.optString("rawInviteIcs").takeIf { it.isNotBlank() },
                isAllDay = item.optBoolean("isAllDay", false),
                reminderMinutesBefore = reminder,
            )
            dao.upsertEvent(entity)
            reminders.schedule(entity)
        }
    }

    private fun buildSyncEngine(): FreedomSyncEngine =
        FreedomSyncEngine(
            context = appContext,
            config = SyncConfig(backend = SyncBackend.LOCAL_ONLY),
            namespace = "calendar",
            backupFileName = "calendar.bin",
        )

    companion object {
        const val DEFAULT_CALENDAR_ID = "local:personal"
    }
}
