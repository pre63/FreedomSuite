package org.freedomsuite.protocol.imap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal class ImapConnection(
    private val host: String,
    private val port: Int,
    private val plainText: Boolean = false,
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    private var tagCounter = 0

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connected = if (plainText) {
                Socket(Proxy.NO_PROXY).apply {
                    connect(InetSocketAddress(this@ImapConnection.host, this@ImapConnection.port), 30_000)
                }
            } else {
                (sslSocketFactory.createSocket() as SSLSocket).apply {
                    connect(InetSocketAddress(this@ImapConnection.host, this@ImapConnection.port), 30_000)
                    startHandshake()
                }
            }
            socket = connected
            reader = BufferedReader(InputStreamReader(connected.inputStream, StandardCharsets.US_ASCII))
            writer = OutputStreamWriter(connected.outputStream, StandardCharsets.US_ASCII)
            val greeting = readLine() ?: error("No IMAP greeting")
            if (!greeting.contains("OK")) error("Unexpected greeting: $greeting")
        }
    }

    suspend fun login(user: String, password: CharArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = command("LOGIN", quote(user), quote(String(password)))
            if (!response.contains("OK")) error("Login failed")
        }
    }

    suspend fun select(folder: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = command("SELECT", quote(folder))
            if (!response.contains("OK")) error("SELECT failed: $response")
        }
    }

    suspend fun listFolders(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = command("LIST", """""", "*")
            response.lineSequence()
                .filter { it.contains("LIST") }
                .mapNotNull { line ->
                    val match = Regex("""LIST \([^)]*\) "(?:\.|/)" "?([^"]+)"?""").find(line)
                    match?.groupValues?.get(1)
                }
                .distinct()
                .sorted()
                .toList()
        }
    }

    suspend fun fetchFolder(folder: String, limit: Int): Result<List<MailMessageSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                select(folder).getOrThrow()
                val uids = searchUids("ALL").takeLast(limit)
                fetchSummaries(uids)
            }
        }

    suspend fun searchFolder(folder: String, query: String, limit: Int): Result<List<MailMessageSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                select(folder).getOrThrow()
                val trimmed = query.trim()
                require(trimmed.isNotEmpty()) { "Search query required" }
                val uids = searchUids("TEXT", quote(trimmed)).takeLast(limit)
                fetchSummaries(uids)
            }
        }

    suspend fun fetchMessageBody(folder: String, uid: Long): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            select(folder).getOrThrow()
            val response = command("UID FETCH", uid.toString(), "(BODY.PEEK[TEXT])")
            extractLiteral(response) ?: ""
        }
    }

    suspend fun fetchRawMessage(folder: String, uid: Long): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            select(folder).getOrThrow()
            val response = command("UID FETCH", uid.toString(), "(BODY.PEEK[])")
            extractLiteral(response) ?: ""
        }
    }

    suspend fun moveToArchive(folder: String, uid: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            select(folder).getOrThrow()
            val folders = listFolders().getOrDefault(emptyList())
            val archiveFolder = folders.firstOrNull {
                it.equals("Archive", ignoreCase = true) ||
                    it.endsWith("/Archive", ignoreCase = true)
            } ?: "Archive"
            val response = command("UID MOVE", uid.toString(), quote(archiveFolder))
            if (!response.contains("OK")) error("MOVE failed: $response")
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching {
            command("LOGOUT")
            socket?.close()
        }
        socket = null
        reader = null
        writer = null
    }

    private fun searchUids(vararg criteria: String): List<Long> {
        val searchResponse = command("UID SEARCH", *criteria)
        return Regex("""\* SEARCH ([\d ]*)""")
            .find(searchResponse)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
    }

    private fun fetchSummaries(uids: List<Long>): List<MailMessageSummary> =
        uids.asReversed().mapNotNull { uid ->
            val fetchResponse = command(
                "UID FETCH",
                uid.toString(),
                "(UID FLAGS ENVELOPE BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE)])",
            )
            parseMessage(uid, fetchResponse)
        }

    private fun command(vararg parts: String): String {
        val tag = "F${++tagCounter}"
        val line = buildString {
            append(tag)
            parts.forEach { part ->
                append(' ')
                append(part)
            }
        }
        writer?.apply {
            write(line)
            write("\r\n")
            flush()
        }
        return readTaggedResponse(tag)
    }

    private fun readTaggedResponse(tag: String): String {
        val lines = mutableListOf<String>()
        while (true) {
            val line = readLine() ?: break
            lines += line
            if (line.startsWith("$tag ")) break
        }
        return lines.joinToString("\n")
    }

    private fun readLine(): String? = reader?.readLine()

    private fun quote(value: String): String =
        if (value.any { it == '"' || it == '\\' || it == '\r' || it == '\n' }) {
            "{${value.toByteArray(StandardCharsets.UTF_8).size}}\r\n$value"
        } else {
            "\"$value\""
        }

    private fun parseMessage(uid: Long, response: String): MailMessageSummary? {
        val envelope = Regex("""ENVELOPE \((.*)\) BODY""").find(response)?.groupValues?.get(1) ?: return null
        val subject = Regex("""NIL|"((?:\\.|[^"\\])*)"""").findAll(envelope).drop(1).firstOrNull()
            ?.groupValues?.get(1)?.unescape() ?: "(no subject)"
        val from = Regex("""\(\("((?:\\.|[^"\\])*)"\s+NIL\)\)""").find(envelope)
            ?.groupValues?.get(1)?.unescape() ?: "unknown"
        val dateHeader = extractHeaderField(response, "Date")
        val dateEpochMs = parseMailDate(dateHeader)
        val snippet = extractHeaderField(response, "Subject").ifBlank { subject }
        return MailMessageSummary(
            uid = uid,
            subject = subject,
            from = from,
            dateEpochMs = dateEpochMs,
            snippet = snippet,
        )
    }

    private fun extractHeaderField(response: String, field: String): String {
        val pattern = Regex("""(?i)$field:\s*(.+)""")
        return response.lineSequence()
            .mapNotNull { pattern.find(it)?.groupValues?.get(1)?.trim() }
            .firstOrNull()
            .orEmpty()
    }

    private fun extractLiteral(response: String): String? {
        val match = Regex("""\{(\d+)\}\r?\n""").find(response) ?: return null
        val length = match.groupValues[1].toInt()
        val start = match.range.last + 1
        return if (response.length >= start + length) {
            response.substring(start, start + length)
        } else {
            null
        }
    }

    private fun String.unescape(): String = replace("\\\"", "\"").replace("\\\\", "\\")

    private fun parseMailDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        return runCatching {
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
                .parse(raw)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}
