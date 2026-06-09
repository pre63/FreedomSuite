package org.freedomsuite.calendar.ui

import org.freedomsuite.calendar.data.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaGrouperTest {
    private val now = AgendaGrouper.startOfDay(1_700_000_000_000L) + 12 * 3_600_000L

    @Test
    fun upcomingGroupsByTodayAndTomorrow() {
        val todayStart = AgendaGrouper.startOfDay(now) + 14 * 3_600_000L
        val tomorrowStart = todayStart + 86_400_000L
        val events = listOf(
            event("a", tomorrowStart, tomorrowStart + 3_600_000L),
            event("b", todayStart, todayStart + 3_600_000L),
        )

        val agenda = AgendaGrouper.buildAgenda(events, AgendaFilter.UPCOMING, nowMs = now)

        assertEquals(4, agenda.size)
        assertTrue(agenda[0] is AgendaRow.Header)
        assertEquals("Today", (agenda[0] as AgendaRow.Header).label)
        assertEquals("b", (agenda[1] as AgendaRow.Event).event.uid)
        assertEquals("Tomorrow", (agenda[2] as AgendaRow.Header).label)
        assertEquals("a", (agenda[3] as AgendaRow.Event).event.uid)
    }

    @Test
    fun pastFilterExcludesFutureEvents() {
        val pastStart = now - 3 * 86_400_000L
        val events = listOf(
            event("old", pastStart, pastStart + 3_600_000L),
            event("future", now + 86_400_000L, now + 90_000_000L),
        )

        val agenda = AgendaGrouper.buildAgenda(events, AgendaFilter.PAST, nowMs = now)

        assertEquals(2, agenda.size)
        assertEquals("old", (agenda[1] as AgendaRow.Event).event.uid)
    }

    @Test
    fun allDayTimeLabel() {
        val start = AgendaGrouper.startOfDay(now)
        val event = event("x", start, AgendaGrouper.endOfDay(start), isAllDay = true)

        val label = AgendaGrouper.formatEventTime(event)

        assertTrue(label.startsWith("All day"))
    }

    @Test
    fun normalizeAllDayRangeCoversFullDays() {
        val start = AgendaGrouper.startOfDay(now)
        val end = start + 86_400_000L
        val (normalizedStart, normalizedEnd) = AgendaGrouper.normalizeAllDayRange(start, end)

        assertEquals(start, normalizedStart)
        assertTrue(normalizedEnd >= AgendaGrouper.endOfDay(end))
    }

    @Test
    fun reminderTriggerSubtractsMinutes() {
        val start = 1_000_000L
        assertEquals(100_000L, org.freedomsuite.calendar.reminder.ReminderOptions.triggerEpochMs(start, 15))
    }

    private fun event(
        uid: String,
        start: Long,
        end: Long,
        isAllDay: Boolean = false,
    ) = EventEntity(
        uid = uid,
        calendarId = "local:personal",
        title = uid,
        description = null,
        location = null,
        startEpochMs = start,
        endEpochMs = end,
        source = "LOCAL",
        responseStatus = "NONE",
        organizer = null,
        sourceMailUid = null,
        rawInviteIcs = null,
        isAllDay = isAllDay,
        reminderMinutesBefore = null,
    )
}
