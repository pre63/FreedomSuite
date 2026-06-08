package org.freedomsuite.testing.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.protocol.caldav.CalDavClientFactory
import org.freedomsuite.protocol.caldav.CalendarEvent
import org.freedomsuite.protocol.imap.ImapClientFactory
import org.freedomsuite.protocol.smtp.OutgoingMessage
import org.freedomsuite.protocol.smtp.SmtpClientFactory
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MailboxOrgEndToEndTest : IntegrationTestBase() {
    @Test
    fun fullMailboxOrgMockFlow() = runBlocking {
        val account = testMailAccount()
        val password = testPassword()

        val imap = ImapClientFactory.create(plainText = true)
        imap.connect(account, password).getOrThrow()
        val inbox = imap.fetchInbox(10).getOrThrow()
        imap.moveToArchive("INBOX", inbox.last().uid).getOrThrow()
        imap.disconnect()

        val smtp = SmtpClientFactory.create(account, plainText = true)
        smtp.send(
            OutgoingMessage(
                from = EmailIdentity(account.email, "Primary", isDefault = true),
                to = listOf("peer@mailbox.test"),
                subject = "E2E",
                bodyText = "All services mocked locally.",
            ),
            password,
        ).getOrThrow()

        val caldav = CalDavClientFactory.create()
        caldav.connect(mock.caldavUrl(), mock.email, password).getOrThrow()
        val calendarUrl = caldav.listCalendars().getOrThrow().first().url
        caldav.createEvent(
            calendarUrl,
            CalendarEvent(
                uid = UUID.randomUUID().toString(),
                title = "E2E event",
                startEpochMs = 1_735_689_600_000,
                endEpochMs = 1_735_693_200_000,
            ),
        ).getOrThrow()

        val context: Context = ApplicationProvider.getApplicationContext()
        val engine = FreedomSyncEngine(
            context,
            SyncConfig(
                backend = SyncBackend.MAILBOX_WEBDAV,
                webDavUrl = mock.webDavUrl(),
                webDavEmail = mock.email,
                webDavPassword = password,
            ),
            namespace = "e2e",
            backupFileName = "suite.bin",
            preferences = InMemorySyncPreferences(),
        )
        val backup = """{"inbox":${inbox.size},"smtp":1}""".toByteArray()
        engine.syncNow(backup, "e2e-passphrase".toCharArray()).getOrThrow()
        val restored = engine.downloadLatest("e2e-passphrase".toCharArray()).getOrThrow()

        assertTrue(inbox.isNotEmpty())
        assertEquals(1, mock.smtp.captured.size)
        assertEquals(String(backup), String(restored))
    }
}
