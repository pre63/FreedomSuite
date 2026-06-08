package org.freedomsuite.testing.integration

import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.protocol.smtp.OutgoingMessage
import org.freedomsuite.protocol.smtp.SmtpClientFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class SmtpIntegrationTest : IntegrationTestBase() {
    @Test
    fun sendMessageToMockServer() = runBlocking {
        val account = testMailAccount()
        val client = SmtpClientFactory.create(account, plainText = true)

        client.send(
            OutgoingMessage(
                from = EmailIdentity(account.email, "Primary", isDefault = true),
                to = listOf("recipient@mailbox.test"),
                subject = "Integration hello",
                bodyText = "Sent via mock SMTP.",
            ),
            testPassword(),
        ).getOrThrow()

        assertEquals(1, mock.smtp.captured.size)
        val captured = mock.smtp.captured.first()
        assertEquals(account.email, captured.from)
        assertEquals("Integration hello", captured.subject)
        assertEquals("Sent via mock SMTP.", captured.body)
    }
}
