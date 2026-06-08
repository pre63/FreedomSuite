package org.freedomsuite.testing.mockserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MockHttpServer {
    private var server: HttpServer? = null
    private val blobs = ConcurrentHashMap<String, ByteArray>()
    private val events = ConcurrentHashMap<String, String>()
    val calendarPath = "/calendars/personal/"

    val port: Int get() = server?.address?.port ?: error("Server not started")
    val baseUrl: String get() = "http://localhost:$port"

    fun start(port: Int = 0) {
        val http = HttpServer.create(InetSocketAddress("localhost", port), 0)
        http.createContext("/") { exchange -> handle(exchange) }
        http.executor = Executors.newCachedThreadPool()
        http.start()
        server = http
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private fun handle(exchange: HttpExchange) {
        if (!isAuthorized(exchange)) {
            exchange.sendResponseHeaders(401, -1)
            exchange.close()
            return
        }

        val method = exchange.requestMethod.uppercase()
        val path = exchange.requestURI.path
        val depth = exchange.requestHeaders.getFirst("Depth") ?: "0"
        when {
            method == "PROPFIND" && depth == "0" -> respond(exchange, 207, connectPropfind())
            method == "PROPFIND" -> respond(exchange, 207, calendarListPropfind())
            method == "REPORT" -> respond(exchange, 207, calendarReport())
            method == "PUT" && path.endsWith(".ics") -> {
                val uid = path.substringAfterLast('/').removeSuffix(".ics")
                val body = exchange.requestBody.readBytes().decodeToString()
                events[uid] = body
                exchange.responseHeaders.add("ETag", "\"$uid\"")
                respond(exchange, 201, "")
            }
            method == "DELETE" && path.endsWith(".ics") -> {
                events.remove(path.substringAfterLast('/').removeSuffix(".ics"))
                respond(exchange, 204, "")
            }
            method == "PUT" -> {
                blobs[path] = exchange.requestBody.readBytes()
                respond(exchange, 200, "")
            }
            method == "GET" && blobs.containsKey(path) -> {
                val bytes = blobs[path]!!
                exchange.responseHeaders.add("Content-Type", "application/octet-stream")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                exchange.close()
            }
            method == "GET" -> respond(exchange, 404, "Not found")
            else -> respond(exchange, 405, "Method not allowed")
        }
    }

    private fun isAuthorized(exchange: HttpExchange): Boolean {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return false
        if (!header.startsWith("Basic ")) return false
        val decoded = String(Base64.getDecoder().decode(header.removePrefix("Basic ")))
        val parts = decoded.split(':', limit = 2)
        return parts.size == 2 && parts[1].isNotEmpty()
    }

    private fun connectPropfind() = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/</d:href>
            <d:propstat>
              <d:prop><d:current-user-principal/></d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
          </d:response>
        </d:multistatus>
    """.trimIndent()

    private fun calendarListPropfind(): String {
        val href = calendarPath
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:response>
                <d:href>$href</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/><cal:calendar/></d:resourcetype>
                    <d:displayname>Personal</d:displayname>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()
    }

    private fun calendarReport(): String {
        if (events.isEmpty()) {
            return """
                <?xml version="1.0" encoding="utf-8"?>
                <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav"/>
            """.trimIndent()
        }
        val responses = events.entries.joinToString("\n") { (uid, ics) ->
            val escaped = ics.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            """
              <d:response>
                <d:href>${calendarPath}$uid.ics</d:href>
                <d:propstat>
                  <d:prop>
                    <d:getetag>"$uid"</d:getetag>
                    <cal:calendar-data>$escaped</cal:calendar-data>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            """.trimIndent()
        }
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              $responses
            </d:multistatus>
        """.trimIndent()
    }

    fun seedEvent(title: String, uid: String = UUID.randomUUID().toString()): String {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:$uid
            DTSTART:20250115T100000Z
            DTEND:20250115T110000Z
            SUMMARY:$title
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n")
        events[uid] = ics
        return uid
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray()
        if (code == 207) {
            exchange.responseHeaders.add("Content-Type", "application/xml; charset=utf-8")
        }
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }
}
