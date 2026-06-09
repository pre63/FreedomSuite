package org.freedomsuite.testing.mockserver

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

data class MockMailMessage(
    val uid: Long,
    val subject: String,
    val from: String,
    val body: String,
    val dateHeader: String = "Sat, 01 Jan 2024 12:00:00 +0000",
)

class MockImapServer(
    private val messages: MutableList<MockMailMessage> = mutableListOf(
        MockMailMessage(1, "Welcome to FreedomSuite", "sender@mailbox.test", "Hello from mock IMAP."),
        MockMailMessage(2, "Second message", "other@mailbox.test", "Archive me."),
    ),
    private val sentMessages: List<MockMailMessage> = listOf(
        MockMailMessage(10, "Sent item", "me@mailbox.test", "Previously sent mail."),
    ),
) {
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    val port: Int get() = serverSocket?.localPort ?: error("Server not started")

    val archivedUids: MutableSet<Long> = mutableSetOf()
    val spamUids: MutableSet<Long> = mutableSetOf()

    fun start(port: Int = 0) {
        check(!running.get()) { "Already started" }
        serverSocket = ServerSocket().also { it.bind(java.net.InetSocketAddress(port)) }
        running.set(true)
        worker = Thread({ acceptLoop() }, "mock-imap").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        serverSocket?.close()
        worker?.join(2_000)
        serverSocket = null
        worker = null
    }

    private fun acceptLoop() {
        while (running.get()) {
            val socket = runCatching { serverSocket?.accept() }.getOrNull() ?: break
            Thread({ handleClient(socket) }, "mock-imap-client").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
            val writer = OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII)
            var selectedFolder = "INBOX"
            writeLine(writer, "* OK mock mailbox.org IMAP ready")
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith("F")) {
                    val tag = line.substringBefore(' ')
                    val upper = line.uppercase()
                    when {
                        upper.contains(" LOGIN ") -> writeLine(writer, "$tag OK LOGIN completed")
                        upper.contains(" SELECT ") -> {
                            selectedFolder = line.substringAfter("SELECT ").trim().trim('"')
                            writeLine(writer, "$tag OK [READ-WRITE] SELECT completed")
                        }
                        upper.contains(" LIST ") -> {
                            writeLine(writer, """* LIST (\HasNoChildren) "/" "INBOX"""")
                            writeLine(writer, """* LIST (\Archive) "/" "Archive"""")
                            writeLine(writer, """* LIST (\Junk) "/" "Spam"""")
                            writeLine(writer, """* LIST (\Sent) "/" "Sent"""")
                            writeLine(writer, "$tag OK LIST completed")
                        }
                        upper.contains(" UID SEARCH ") -> {
                            val active = activeMessages(selectedFolder)
                            val uids = if (upper.contains(" TEXT ")) {
                                val query = extractQuotedSearch(line).lowercase()
                                active.filter { message ->
                                    message.subject.lowercase().contains(query) ||
                                        message.from.lowercase().contains(query) ||
                                        message.body.lowercase().contains(query)
                                }.joinToString(" ") { it.uid.toString() }
                            } else {
                                active.joinToString(" ") { it.uid.toString() }
                            }
                            writeLine(writer, "* SEARCH $uids")
                            writeLine(writer, "$tag OK SEARCH completed")
                        }
                        upper.contains(" UID FETCH ") && upper.contains("BODY.PEEK[TEXT]") -> {
                            val uid = upper.substringAfter("UID FETCH ").substringBefore(' ').toLong()
                            val body = activeMessages(selectedFolder).first { it.uid == uid }.body
                            writeLine(writer, "* $uid FETCH (BODY[TEXT] {${body.length}}")
                            writer.write(body)
                            writer.write("\r\n")
                            writer.flush()
                            writeLine(writer, "$tag OK FETCH completed")
                        }
                        upper.contains(" UID FETCH ") && upper.contains("BODY.PEEK[]") -> {
                            val uid = upper.substringAfter("UID FETCH ").substringBefore(' ').toLong()
                            val message = activeMessages(selectedFolder).first { it.uid == uid }
                            val raw = buildString {
                                append("Content-Type: text/plain; charset=UTF-8\r\n")
                                append("\r\n")
                                append(message.body)
                            }
                            writeLine(writer, "* $uid FETCH (BODY[] {${raw.length}}")
                            writer.write(raw)
                            writer.write("\r\n")
                            writer.flush()
                            writeLine(writer, "$tag OK FETCH completed")
                        }
                        upper.contains(" UID FETCH ") -> {
                            val uid = upper.substringAfter("UID FETCH ").substringBefore(' ').toLong()
                            val message = activeMessages(selectedFolder).first { it.uid == uid }
                            val header = "Subject: ${message.subject}\r\nFrom: ${message.from}\r\nDate: ${message.dateHeader}\r\n"
                            val envelope = """("${message.dateHeader}" "${message.subject}" (("${message.from}" NIL)) NIL NIL NIL NIL)"""
                            writeLine(
                                writer,
                                """* $uid FETCH (UID $uid FLAGS () ENVELOPE $envelope BODY[HEADER.FIELDS (SUBJECT FROM DATE)] {${header.length}}""",
                            )
                            writer.write(header)
                            writer.write("\r\n")
                            writer.flush()
                            writeLine(writer, "$tag OK FETCH completed")
                        }
                        upper.contains(" UID MOVE ") -> {
                            val parts = line.split(' ')
                            val uid = parts[parts.indexOf("MOVE") + 1].toLong()
                            val destination = line.substringAfter("MOVE $uid ").trim().trim('"')
                            when {
                                destination.equals("Archive", ignoreCase = true) -> {
                                    archivedUids += uid
                                    spamUids -= uid
                                }
                                destination.equals("Spam", ignoreCase = true) ||
                                    destination.equals("Junk", ignoreCase = true) -> {
                                    spamUids += uid
                                    archivedUids -= uid
                                }
                                destination.equals("INBOX", ignoreCase = true) -> {
                                    spamUids -= uid
                                    archivedUids -= uid
                                }
                            }
                            writeLine(writer, "$tag OK MOVE completed")
                        }
                        upper.contains(" LOGOUT") -> {
                            writeLine(writer, "* BYE mock IMAP logging out")
                            writeLine(writer, "$tag OK LOGOUT completed")
                            break
                        }
                        else -> writeLine(writer, "$tag OK")
                    }
                }
            }
        }
    }

    private fun activeMessages(folder: String): List<MockMailMessage> = when {
        folder.equals("Archive", ignoreCase = true) ->
            messages.filter { it.uid in archivedUids }
        folder.equals("Spam", ignoreCase = true) || folder.equals("Junk", ignoreCase = true) ->
            messages.filter { it.uid in spamUids }
        folder.equals("Sent", ignoreCase = true) ->
            sentMessages
        else ->
            messages.filter { it.uid !in archivedUids && it.uid !in spamUids }
    }

    private fun extractQuotedSearch(line: String): String {
        val match = Regex("""TEXT\s+"([^"]*)"""").find(line)
        return match?.groupValues?.get(1) ?: line.substringAfter("TEXT ").trim().trim('"')
    }

    private fun writeLine(writer: OutputStreamWriter, line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }
}
