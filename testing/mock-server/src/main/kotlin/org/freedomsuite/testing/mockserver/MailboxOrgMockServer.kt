package org.freedomsuite.testing.mockserver

/**
 * Local stand-in for IMAP, SMTP, CalDAV, WebDAV, and S3-compatible sync.
 * Uses plain TCP/HTTP (no TLS). Ephemeral ports by default; fixed ports for [DevMailServer].
 */
class MailboxOrgMockServer(
    val email: String = DevMailDefaults.EMAIL,
    val password: String = DevMailDefaults.PASSWORD,
) {
    val imap = MockImapServer()
    val smtp = MockSmtpServer()
    val http = MockHttpServer()

    fun start(
        imapPort: Int = 0,
        smtpPort: Int = 0,
        httpPort: Int = 0,
    ) {
        imap.start(imapPort)
        smtp.start(smtpPort)
        http.start(httpPort)
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
