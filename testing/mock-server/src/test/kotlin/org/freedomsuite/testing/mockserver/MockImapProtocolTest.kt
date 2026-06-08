package org.freedomsuite.testing.mockserver

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.nio.charset.StandardCharsets

class MockImapProtocolTest {
    @Test
    fun imapMockHandlesLoginSearchAndFetch() {
        val server = MockImapServer()
        server.start()
        Socket(Proxy.NO_PROXY).use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", server.port), 2_000)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII)
            assertTrue(reader.readLine()!!.contains("OK"))
            write(writer, "F1 LOGIN \"test@mailbox.org\" \"secret\"")
            assertTrue(readUntilTag(reader, "F1").contains("OK"))
            write(writer, "F2 SELECT \"INBOX\"")
            assertTrue(readUntilTag(reader, "F2").contains("OK"))
            write(writer, "F3 UID SEARCH ALL")
            val search = readUntilTag(reader, "F3")
            assertTrue(search.contains("SEARCH 1 2"))
            write(writer, "F4 LOGOUT")
            readUntilTag(reader, "F4")
        }
        server.stop()
    }

    private fun write(writer: OutputStreamWriter, line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    private fun readUntilTag(reader: BufferedReader, tag: String): String {
        val lines = mutableListOf<String>()
        while (true) {
            val line = reader.readLine() ?: break
            lines += line
            if (line.startsWith("$tag ")) break
        }
        return lines.joinToString("\n")
    }
}
