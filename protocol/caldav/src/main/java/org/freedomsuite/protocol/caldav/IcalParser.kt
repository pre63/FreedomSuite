package org.freedomsuite.protocol.caldav

import org.freedomsuite.protocol.ical.IcalParser as CoreIcalParser

internal object IcalParser {
    fun parseEvents(ics: String): List<CalendarEvent> =
        CoreIcalParser.parseEvents(ics).map { event ->
            CalendarEvent(
                uid = event.uid,
                title = event.title,
                description = event.description,
                location = event.location,
                startEpochMs = event.startEpochMs,
                endEpochMs = event.endEpochMs,
            )
        }

    fun buildEvent(event: CalendarEvent): String =
        CoreIcalParser.buildEvent(
            org.freedomsuite.protocol.ical.CalendarEvent(
                uid = event.uid,
                title = event.title,
                description = event.description,
                location = event.location,
                startEpochMs = event.startEpochMs,
                endEpochMs = event.endEpochMs,
            ),
        )
}
