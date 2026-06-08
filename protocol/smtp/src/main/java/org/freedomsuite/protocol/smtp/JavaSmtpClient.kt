package org.freedomsuite.protocol.smtp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.freedomsuite.core.account.MailAccount
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal class JavaSmtpClient(
    private val account: MailAccount,
    private val plainText: Boolean = false,
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
) : SmtpClient {
    override suspend fun send(message: OutgoingMessage, password: CharArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val socket = if (plainText) {
                    Socket(Proxy.NO_PROXY).apply {
                        connect(InetSocketAddress(account.smtpHost, account.smtpPort), 30_000)
                    }
                } else {
                    (sslSocketFactory.createSocket() as SSLSocket).apply {
                        connect(InetSocketAddress(account.smtpHost, account.smtpPort), 30_000)
                        startHandshake()
                    }
                }
                socket.use { connected ->
                    val reader = BufferedReader(InputStreamReader(connected.inputStream, StandardCharsets.US_ASCII))
                    val writer = OutputStreamWriter(connected.outputStream, StandardCharsets.US_ASCII)
                    expect(reader, 220)
                    writeLine(writer, "EHLO freedom-suite")
                    readUntil(reader, "250")
                    writeLine(writer, "AUTH LOGIN")
                    expect(reader, 334)
                    writeLine(writer, Base64.getEncoder().encodeToString(account.email.toByteArray()))
                    expect(reader, 334)
                    writeLine(writer, Base64.getEncoder().encodeToString(String(password).toByteArray()))
                    expect(reader, 235)
                    writeLine(writer, "MAIL FROM:<${message.from.address}>")
                    expect(reader, 250)
                    message.to.forEach { recipient ->
                        writeLine(writer, "RCPT TO:<$recipient>")
                        expect(reader, 250)
                    }
                    writeLine(writer, "DATA")
                    expect(reader, 354)
                    val body = if (message.calendarReplyIcs != null) {
                        buildMultipartReply(message)
                    } else {
                        buildString {
                            append("From: ${message.from.address}\r\n")
                            append("To: ${message.to.joinToString(", ")}\r\n")
                            append("Subject: ${message.subject}\r\n")
                            message.inReplyTo?.let { append("In-Reply-To: $it\r\n") }
                            append("Content-Type: text/plain; charset=UTF-8\r\n")
                            append("\r\n")
                            append(message.bodyText.replace("\n", "\r\n"))
                            append("\r\n.\r\n")
                        }
                    }
                    writer.write(body)
                    writer.flush()
                    expect(reader, 250)
                    writeLine(writer, "QUIT")
                }
            }
        }

    private fun writeLine(writer: OutputStreamWriter, line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    private fun expect(reader: BufferedReader, code: Int) {
        val line = reader.readLine() ?: error("SMTP connection closed")
        if (!line.startsWith(code.toString())) error("SMTP error: $line")
    }

    private fun readUntil(reader: BufferedReader, code: String) {
        while (true) {
            val line = reader.readLine() ?: break
            if (line.startsWith(code)) {
                if (!line.contains("-")) break
            }
        }
    }

    private fun buildMultipartReply(message: OutgoingMessage): String {
        val boundary = "freedom-suite-${System.currentTimeMillis()}"
        val ics = message.calendarReplyIcs ?: error("calendarReplyIcs required")
        return buildString {
            append("From: ${message.from.address}\r\n")
            append("To: ${message.to.joinToString(", ")}\r\n")
            append("Subject: ${message.subject}\r\n")
            message.inReplyTo?.let { append("In-Reply-To: $it\r\n") }
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n")
            append("\r\n")
            append("--$boundary\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("\r\n")
            append(message.bodyText.replace("\n", "\r\n"))
            append("\r\n")
            append("--$boundary\r\n")
            append("Content-Type: text/calendar; method=REPLY; charset=UTF-8\r\n")
            append("Content-Disposition: attachment; filename=\"invite.ics\"\r\n")
            append("\r\n")
            append(ics.replace("\n", "\r\n"))
            append("\r\n")
            append("--$boundary--\r\n")
            append(".\r\n")
        }
    }
}
