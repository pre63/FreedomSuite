package org.freedomsuite.core.account.discovery

import org.freedomsuite.core.network.MailboxOrgDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MailProviderPresetsTest {
    @Test
    fun forDomain_returnsMailboxOrgPreset() {
        val settings = MailProviderPresets.forDomain("mailbox.org")

        assertNotNull(settings)
        assertEquals(MailboxOrgDefaults.IMAP_HOST, settings?.imapHost)
        assertEquals(MailboxOrgDefaults.SMTP_HOST, settings?.smtpHost)
        assertEquals(DiscoverySource.PRESET, settings?.source)
    }

    @Test
    fun forEmail_isCaseInsensitive() {
        val settings = MailProviderPresets.forEmail("User@Gmail.com")

        assertNotNull(settings)
        assertEquals("imap.gmail.com", settings?.imapHost)
        assertEquals("smtp.gmail.com", settings?.smtpHost)
    }

    @Test
    fun forDomain_returnsNullForUnknownDomain() {
        assertNull(MailProviderPresets.forDomain("unknown-provider.example"))
    }

    @Test
    fun forDomain_returnsDevMailPreset() {
        val settings = MailProviderPresets.forDomain("freedom.test")

        assertNotNull(settings)
        assertEquals(DevMailServer.EMULATOR_HOST, settings?.imapHost)
        assertEquals(DevMailServer.IMAP_PORT, settings?.imapPort)
        assertEquals(DevMailServer.SMTP_PORT, settings?.smtpPort)
        assertEquals(true, settings?.plainText)
    }
}
