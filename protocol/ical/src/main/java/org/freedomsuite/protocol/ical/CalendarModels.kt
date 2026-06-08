package org.freedomsuite.protocol.ical

enum class InviteMethod {
    REQUEST,
    REPLY,
    CANCEL,
    PUBLISH,
    UNKNOWN,
}

enum class InviteResponseStatus {
    NEEDS_ACTION,
    ACCEPTED,
    TENTATIVE,
    DECLINED,
    NONE,
}

data class CalendarEvent(
    val uid: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val organizer: String? = null,
    val sequence: Int = 0,
)

data class CalendarInvite(
    val method: InviteMethod,
    val event: CalendarEvent,
    val rawIcs: String,
)
