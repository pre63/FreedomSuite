package org.freedomsuite.testing.mockserver

/**
 * Local stand-in for mailbox.org IMAP, SMTP, CalDAV, WebDAV, and S3-compatible sync.
 * Binds ephemeral ports on 127.0.0.1 using plain TCP/HTTP for integration tests.
 */
class MailboxOrgMockServer {
    val imap = MockImapServer()
    val smtp = MockSmtpServer()
    val http = MockHttpServer()

    val email: String = "test@mailbox.org"
    val password: String = "test-app-password"

    fun start() {
        imap.start()
        smtp.start()
        http.start()
        Thread.sleep(50)
    }

    fun stop() {
        imap.stop()
        smtp.stop()
        http.stop()
    }

    fun imapHost(): String = "127.0.0.1"
    fun imapPort(): Int = imap.port

    fun smtpHost(): String = "127.0.0.1"
    fun smtpPort(): Int = smtp.port

    fun caldavUrl(): String = http.baseUrl

    fun webDavUrl(): String = http.baseUrl

    fun s3Endpoint(): String = http.baseUrl
    fun s3Bucket(): String = "test-bucket"
}
