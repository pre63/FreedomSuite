package org.freedomsuite.core.account.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MailAutoconfigDiscoveryTest {
    private val discovery = MailAutoconfigDiscovery()

    @Test
    fun parseAutoconfig_extractsImapAndSmtp() {
        val xml = """
            <?xml version="1.0"?>
            <clientConfig version="1.1">
              <emailProvider id="example.com">
                <incomingServer type="imap">
                  <hostname>imap.example.com</hostname>
                  <port>993</port>
                </incomingServer>
                <outgoingServer type="smtp">
                  <hostname>smtp.example.com</hostname>
                  <port>587</port>
                </outgoingServer>
              </emailProvider>
            </clientConfig>
        """.trimIndent()

        val settings = discovery.parseAutoconfig(xml)

        assertNotNull(settings)
        assertEquals("imap.example.com", settings?.imapHost)
        assertEquals(993, settings?.imapPort)
        assertEquals("smtp.example.com", settings?.smtpHost)
        assertEquals(587, settings?.smtpPort)
        assertEquals(DiscoverySource.AUTOCONFIG, settings?.source)
    }

    @Test
    fun parseAutoconfig_returnsNullWhenImapMissing() {
        val xml = """
            <clientConfig>
              <outgoingServer type="smtp">
                <hostname>smtp.example.com</hostname>
                <port>587</port>
              </outgoingServer>
            </clientConfig>
        """.trimIndent()

        assertNull(discovery.parseAutoconfig(xml))
    }
}
