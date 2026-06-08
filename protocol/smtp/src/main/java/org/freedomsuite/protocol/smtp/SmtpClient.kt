package org.freedomsuite.protocol.smtp

import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.core.account.MailAccount
import javax.net.ssl.SSLSocketFactory

data class OutgoingMessage(
    val from: EmailIdentity,
    val to: List<String>,
    val subject: String,
    val bodyText: String,
    val inReplyTo: String? = null,
    val calendarReplyIcs: String? = null,
)

interface SmtpClient {
    suspend fun send(message: OutgoingMessage, password: CharArray): Result<Unit>
}

object SmtpClientFactory {
    fun create(
        account: MailAccount,
        plainText: Boolean = false,
        sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
    ): SmtpClient = JavaSmtpClient(account, plainText, sslSocketFactory)
}
