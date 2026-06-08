package org.freedomsuite.testing.mockserver

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

data class CapturedSmtpMessage(
    val from: String,
    val to: List<String>,
    val subject: String,
    val body: String,
)

class MockSmtpServer {
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    val captured: MutableList<CapturedSmtpMessage> = mutableListOf()

    val port: Int get() = serverSocket?.localPort ?: error("Server not started")

    fun start(port: Int = 0) {
        check(!running.get()) { "Already started" }
        serverSocket = ServerSocket().also { it.bind(java.net.InetSocketAddress(port)) }
        running.set(true)
        worker = Thread({ acceptLoop() }, "mock-smtp").apply {
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
            Thread({ handleClient(socket) }, "mock-smtp-client").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
            val writer = OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII)
            writeLine(writer, "220 mock.mailbox.test ESMTP ready")
            var mailFrom = ""
            val recipients = mutableListOf<String>()
            val dataLines = mutableListOf<String>()
            var inData = false
            var awaitingAuth = 0

            while (true) {
                val line = reader.readLine() ?: break
                val upper = line.uppercase()
                when {
                    upper.startsWith("EHLO") -> {
                        writeLine(writer, "250-mock.mailbox.test")
                        writeLine(writer, "250 AUTH LOGIN")
                    }
                    upper == "AUTH LOGIN" -> {
                        awaitingAuth = 1
                        writeLine(writer, "334 VXNlcm5hbWU6")
                    }
                    awaitingAuth == 1 -> {
                        awaitingAuth = 2
                        writeLine(writer, "334 UGFzc3dvcmQ6")
                    }
                    awaitingAuth == 2 -> {
                        awaitingAuth = 0
                        writeLine(writer, "235 Authentication successful")
                    }
                    upper.startsWith("MAIL FROM:") -> {
                        mailFrom = line.substringAfter('<').substringBefore('>')
                        writeLine(writer, "250 OK")
                    }
                    upper.startsWith("RCPT TO:") -> {
                        recipients += line.substringAfter('<').substringBefore('>')
                        writeLine(writer, "250 OK")
                    }
                    upper == "DATA" -> {
                        inData = true
                        writeLine(writer, "354 End data with <CR><LF>.<CR><LF>")
                    }
                    inData && line == "." -> {
                        inData = false
                        val subject = dataLines.firstOrNull { it.startsWith("Subject:") }
                            ?.substringAfter("Subject:")?.trim().orEmpty()
                        val body = dataLines.dropWhile { it.isNotBlank() }.drop(1).joinToString("\n")
                        captured += CapturedSmtpMessage(mailFrom, recipients.toList(), subject, body)
                        writeLine(writer, "250 OK")
                    }
                    inData -> dataLines += line
                    upper == "QUIT" -> {
                        writeLine(writer, "221 Bye")
                        break
                    }
                    else -> writeLine(writer, "250 OK")
                }
            }
        }
    }

    private fun writeLine(writer: OutputStreamWriter, line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }
}
