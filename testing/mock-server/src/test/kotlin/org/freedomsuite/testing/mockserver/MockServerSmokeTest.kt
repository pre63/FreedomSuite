package org.freedomsuite.testing.mockserver

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket

class MockServerSmokeTest {
    @Test
    fun imapServerAcceptsTcpConnection() {
        val server = MockImapServer()
        server.start()
        Socket(Proxy.NO_PROXY).use {
            it.connect(InetSocketAddress("127.0.0.1", server.port), 2_000)
        }
        assertTrue(server.port > 0)
        server.stop()
    }
}
