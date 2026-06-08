package org.freedomsuite.protocol.ical

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IcalParser {
    fun parseInvite(ics: String): CalendarInvite? {
        val unfolded = unfold(ics)
        val method = parseMethod(unfolded)
        val event = parseEvents(unfolded).firstOrNull() ?: return null
        return CalendarInvite(method = method, event = event, rawIcs = ics)
    }

    fun parseEvents(ics: String): List<CalendarEvent> {
        val unfolded = unfold(ics)
        return unfolded.split("BEGIN:VEVENT")
            .drop(1)
            .mapNotNull { block ->
                val segment = "BEGIN:VEVENT$block"
                val uid = field(segment, "UID") ?: return@mapNotNull null
                val title = field(segment, "SUMMARY") ?: "(no title)"
                val start = parseDate(field(segment, "DTSTART")) ?: return@mapNotNull null
                val end = parseDate(field(segment, "DTEND"))
                    ?: parseDate(field(segment, "DTSTART"))
                    ?: start + 3_600_000
                CalendarEvent(
                    uid = uid,
                    title = title,
                    description = field(segment, "DESCRIPTION"),
                    location = field(segment, "LOCATION"),
                    startEpochMs = start,
                    endEpochMs = end,
                    organizer = field(segment, "ORGANIZER")?.let(::cleanMailto),
                    sequence = field(segment, "SEQUENCE")?.toIntOrNull() ?: 0,
                )
            }
    }

    fun buildEvent(event: CalendarEvent): String {
        val start = formatUtc(event.startEpochMs)
        val end = formatUtc(event.endEpochMs)
        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//Freedom Suite//Calendar//EN\r\n")
            append("BEGIN:VEVENT\r\n")
            append("UID:").append(event.uid).append("\r\n")
            append("DTSTAMP:").append(formatUtc(System.currentTimeMillis())).append("\r\n")
            append("DTSTART:").append(start).append("\r\n")
            append("DTEND:").append(end).append("\r\n")
            append("SUMMARY:").append(escape(event.title)).append("\r\n")
            event.description?.let { append("DESCRIPTION:").append(escape(it)).append("\r\n") }
            event.location?.let { append("LOCATION:").append(escape(it)).append("\r\n") }
            event.organizer?.let { append("ORGANIZER:mailto:").append(it).append("\r\n") }
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
    }

    fun buildReply(
        invite: CalendarInvite,
        attendeeEmail: String,
        response: InviteResponseStatus,
    ): String {
        val partStat = when (response) {
            InviteResponseStatus.ACCEPTED -> "ACCEPTED"
            InviteResponseStatus.TENTATIVE -> "TENTATIVE"
            InviteResponseStatus.DECLINED -> "DECLINED"
            else -> "NEEDS-ACTION"
        }
        val event = invite.event
        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//Freedom Suite//Calendar//EN\r\n")
            append("METHOD:REPLY\r\n")
            append("BEGIN:VEVENT\r\n")
            append("UID:").append(event.uid).append("\r\n")
            append("DTSTAMP:").append(formatUtc(System.currentTimeMillis())).append("\r\n")
            append("DTSTART:").append(formatUtc(event.startEpochMs)).append("\r\n")
            append("DTEND:").append(formatUtc(event.endEpochMs)).append("\r\n")
            append("SUMMARY:").append(escape(event.title)).append("\r\n")
            event.organizer?.let { append("ORGANIZER:mailto:").append(it).append("\r\n") }
            append("ATTENDEE;PARTSTAT=").append(partStat).append(":mailto:")
                .append(attendeeEmail).append("\r\n")
            append("SEQUENCE:").append(event.sequence).append("\r\n")
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
    }

    private fun parseMethod(ics: String): InviteMethod {
        val raw = field(ics, "METHOD")?.uppercase(Locale.US) ?: return InviteMethod.UNKNOWN
        return when (raw) {
            "REQUEST" -> InviteMethod.REQUEST
            "REPLY" -> InviteMethod.REPLY
            "CANCEL" -> InviteMethod.CANCEL
            "PUBLISH" -> InviteMethod.PUBLISH
            else -> InviteMethod.UNKNOWN
        }
    }

    private fun unfold(text: String): String = text.replace(Regex("""\r?\n[ \t]"""), "")

    private fun field(block: String, name: String): String? {
        val regex = Regex("""(?m)^$name(?:;[^:]*)?:(.*)$""")
        return regex.find(block)?.groupValues?.get(1)?.trim()?.unescape()
    }

    private fun parseDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace("Z", "")
        val patterns = listOf("yyyyMMdd'T'HHmmss", "yyyyMMdd")
        for (pattern in patterns) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            runCatching { format.parse(cleaned)?.time }?.getOrNull()?.let { return it }
        }
        return null
    }

    private fun formatUtc(epochMs: Long): String {
        val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(epochMs)
    }

    private fun cleanMailto(value: String): String =
        value.removePrefix("mailto:").trim()

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    private fun String.unescape(): String =
        replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";")
}
