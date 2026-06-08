package org.freedomsuite.protocol.caldav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.freedomsuite.core.network.PrivacyHttpClient

internal class OkHttpCalDavClient(
    client: OkHttpClient? = null,
) : CalDavClient {
    private val client = client ?: PrivacyHttpClient.create(
        connectTimeoutSeconds = 30,
        readTimeoutSeconds = 60,
    )

    private var baseUrl: String = ""
    private var authHeader: String = ""

    override suspend fun connect(baseUrl: String, email: String, password: CharArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = baseUrl.trimEnd('/')
                val header = Credentials.basic(email, String(password))
                val request = Request.Builder()
                    .url(normalized)
                    .header("Authorization", header)
                    .method(
                        "PROPFIND",
                        propfindBody().toRequestBody("application/xml".toMediaType()),
                    )
                    .header("Depth", "0")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful && response.code != 207) {
                    error("CalDAV connect failed: ${response.code}")
                }
                this@OkHttpCalDavClient.baseUrl = normalized
                authHeader = header
            }
        }

    override suspend fun listCalendars(): Result<List<CalendarInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(baseUrl)
                .header("Authorization", authHeader)
                .method(
                    "PROPFIND",
                    calendarPropfindBody().toRequestBody("application/xml".toMediaType()),
                )
                .header("Depth", "1")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful && response.code != 207) error("listCalendars failed: ${response.code}")
            parseCalendars(body)
        }
    }

    override suspend fun fetchEvents(calendarUrl: String, startEpochMs: Long, endEpochMs: Long): Result<List<CalendarEvent>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(calendarUrl)
                    .header("Authorization", authHeader)
                    .method(
                        "REPORT",
                        calendarQueryBody(startEpochMs, endEpochMs).toRequestBody("application/xml".toMediaType()),
                    )
                    .header("Depth", "1")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful && response.code != 207) error("fetchEvents failed: ${response.code}")
                parseEventsResponse(body)
            }
        }

    override suspend fun createEvent(calendarUrl: String, event: CalendarEvent): Result<CalendarEvent> =
        withContext(Dispatchers.IO) {
            runCatching {
                val href = "${calendarUrl.trimEnd('/')}/${event.uid}.ics"
                val ics = IcalParser.buildEvent(event)
                val request = Request.Builder()
                    .url(href)
                    .header("Authorization", authHeader)
                    .put(ics.toRequestBody("text/calendar".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) error("createEvent failed: ${response.code}")
                event.copy(href = href, etag = response.header("ETag"))
            }
        }

    override suspend fun updateEvent(event: CalendarEvent): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val href = event.href ?: error("Missing href")
            val ics = IcalParser.buildEvent(event)
            val builder = Request.Builder()
                .url(href)
                .header("Authorization", authHeader)
                .put(ics.toRequestBody("text/calendar".toMediaType()))
            event.etag?.let { builder.header("If-Match", it) }
            val response = client.newCall(builder.build()).execute()
            if (!response.isSuccessful) error("updateEvent failed: ${response.code}")
        }
    }

    override suspend fun deleteEvent(event: CalendarEvent): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val href = event.href ?: error("Missing href")
            val builder = Request.Builder()
                .url(href)
                .header("Authorization", authHeader)
                .delete()
            event.etag?.let { builder.header("If-Match", it) }
            val response = client.newCall(builder.build()).execute()
            if (!response.isSuccessful && response.code != 404) error("deleteEvent failed: ${response.code}")
        }
    }

    private fun parseCalendars(xml: String): List<CalendarInfo> {
        val responses = xml.split("<d:response>").drop(1)
        return responses.mapNotNull { chunk ->
            val href = Regex("""<d:href>([^<]+)</d:href>""").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            if (!chunk.contains("calendar")) return@mapNotNull null
            val displayName = Regex("""<d:displayname>([^<]*)</d:displayname>""")
                .find(chunk)?.groupValues?.get(1) ?: href.substringAfterLast('/')
            CalendarInfo(url = resolveHref(href), displayName = displayName)
        }.distinctBy { it.url }
    }

    private fun parseEventsResponse(xml: String): List<CalendarEvent> {
        val chunks = xml.split("<d:response>").drop(1)
        return chunks.flatMap { chunk ->
            val href = Regex("""<d:href>([^<]+)</d:href>""").find(chunk)?.groupValues?.get(1)
            val etag = Regex("""<d:getetag>([^<]*)</d:getetag>""").find(chunk)?.groupValues?.get(1)
            val ics = Regex("""<cal:calendar-data>([\s\S]*?)</cal:calendar-data>""")
                .find(chunk)?.groupValues?.get(1)
                ?: Regex("""<C:calendar-data>([\s\S]*?)</C:calendar-data>""")
                    .find(chunk)?.groupValues?.get(1)
                ?: return@flatMap emptyList()
            IcalParser.parseEvents(unescapeXml(ics)).map { event ->
                event.copy(
                    href = href?.let { resolveHref(it) },
                    etag = etag,
                )
            }
        }
    }

    private fun resolveHref(href: String): String {
        if (href.startsWith("http")) return href
        return baseUrl.trimEnd('/') + href
    }

    private fun unescapeXml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    private fun propfindBody() = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:propfind xmlns:d="DAV:">
          <d:prop>
            <d:current-user-principal/>
          </d:prop>
        </d:propfind>
    """.trimIndent()

    private fun calendarPropfindBody() = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:propfind xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
          <d:prop>
            <d:resourcetype/>
            <d:displayname/>
          </d:prop>
        </d:propfind>
    """.trimIndent()

    private fun calendarQueryBody(startEpochMs: Long, endEpochMs: Long) = """
        <?xml version="1.0" encoding="utf-8"?>
        <cal:calendar-query xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
          <d:prop>
            <d:getetag/>
            <cal:calendar-data/>
          </d:prop>
          <cal:filter>
            <cal:comp-filter name="VCALENDAR">
              <cal:comp-filter name="VEVENT">
                <cal:time-range start="${formatUtc(startEpochMs)}" end="${formatUtc(endEpochMs)}"/>
              </cal:comp-filter>
            </cal:comp-filter>
          </cal:filter>
        </cal:calendar-query>
    """.trimIndent()

    private fun formatUtc(epochMs: Long): String {
        val format = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(epochMs)
    }
}
