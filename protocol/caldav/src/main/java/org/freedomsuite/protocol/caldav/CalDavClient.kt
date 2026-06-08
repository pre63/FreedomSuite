package org.freedomsuite.protocol.caldav

interface CalDavClient {
    suspend fun connect(baseUrl: String, email: String, password: CharArray): Result<Unit>
    suspend fun listCalendars(): Result<List<CalendarInfo>>
    suspend fun fetchEvents(calendarUrl: String, startEpochMs: Long, endEpochMs: Long): Result<List<CalendarEvent>>
    suspend fun createEvent(calendarUrl: String, event: CalendarEvent): Result<CalendarEvent>
    suspend fun updateEvent(event: CalendarEvent): Result<Unit>
    suspend fun deleteEvent(event: CalendarEvent): Result<Unit>
}

data class CalendarInfo(val url: String, val displayName: String)

data class CalendarEvent(
    val uid: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val etag: String? = null,
    val href: String? = null,
)

object CalDavClientFactory {
    fun create(httpClient: okhttp3.OkHttpClient? = null): CalDavClient = OkHttpCalDavClient(httpClient)
}
