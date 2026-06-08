package org.freedomsuite.testing.integration

import kotlinx.coroutines.runBlocking
import org.freedomsuite.protocol.caldav.CalDavClientFactory
import org.freedomsuite.protocol.caldav.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CalDavIntegrationTest : IntegrationTestBase() {
    @Test
    fun listAndFetchEventsFromMockServer() = runBlocking {
        mock.http.seedEvent("Standup")
        val client = CalDavClientFactory.create()

        client.connect(mock.caldavUrl(), mock.email, testPassword()).getOrThrow()
        val calendars = client.listCalendars().getOrThrow()
        assertTrue(calendars.isNotEmpty())

        val calendarUrl = calendars.first().url
        val events = client.fetchEvents(
            calendarUrl = calendarUrl,
            startEpochMs = 0L,
            endEpochMs = System.currentTimeMillis() + 365L * 86_400_000,
        ).getOrThrow()

        assertEquals(1, events.size)
        assertEquals("Standup", events.first().title)
    }

    @Test
    fun createAndDeleteEventOnMockServer() = runBlocking {
        val client = CalDavClientFactory.create()
        client.connect(mock.caldavUrl(), mock.email, testPassword()).getOrThrow()
        val calendarUrl = client.listCalendars().getOrThrow().first().url

        val uid = UUID.randomUUID().toString()
        val created = client.createEvent(
            calendarUrl = calendarUrl,
            event = CalendarEvent(
                uid = uid,
                title = "New event",
                startEpochMs = 1_735_689_600_000,
                endEpochMs = 1_735_693_200_000,
            ),
        ).getOrThrow()

        assertTrue(created.href!!.endsWith("$uid.ics"))

        val events = client.fetchEvents(calendarUrl, 0L, Long.MAX_VALUE).getOrThrow()
        assertEquals(1, events.size)

        client.deleteEvent(created).getOrThrow()
        val afterDelete = client.fetchEvents(calendarUrl, 0L, Long.MAX_VALUE).getOrThrow()
        assertTrue(afterDelete.isEmpty())
    }
}
