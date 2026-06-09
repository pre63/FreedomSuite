package org.freedomsuite.testing.mockserver

/**
 * Local plain-text IMAP/SMTP server for manual Inbox testing on emulator or device.
 *
 * Run: `./gradlew :testing:mock-server:run` or `make dev-mail-server`
 */
fun main() {
    val server = DevMailServer()
    try {
        server.start()
    } catch (e: java.net.BindException) {
        System.err.println(
            """
            |
            |  Could not bind IMAP ${DevMailDefaults.IMAP_PORT} / SMTP ${DevMailDefaults.SMTP_PORT}.
            |  Another dev mail server may already be running (check: lsof -i :${DevMailDefaults.IMAP_PORT}).
            |  Stop it with Ctrl+C in that terminal, or: kill $(lsof -ti :${DevMailDefaults.IMAP_PORT})
            |
            """.trimMargin(),
        )
        throw e
    }
    printBanner(server)
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    Thread.currentThread().join()
}

private fun printBanner(server: DevMailServer) {
    val imapPort = server.imapPort()
    val smtpPort = server.smtpPort()
    println(
        """
        |
        |  Freedom Suite — dev mail server
        |  ───────────────────────────────
        |  IMAP  0.0.0.0:$imapPort  (plain, no TLS)
        |  SMTP  0.0.0.0:$smtpPort  (plain, no TLS)
        |
        |  Credentials (any login is accepted; use these in the app):
        |    Email:    ${server.email}
        |    Password: ${server.password}
        |
        |  Inbox app (emulator):
        |    Enter the email above — discovery uses preset for @freedom.test
        |
        |  Inbox app (physical device on same Wi‑Fi):
        |    Manual settings → IMAP/SMTP host = this computer's LAN IP
        |    Ports: IMAP $imapPort, SMTP $smtpPort
        |
        |  Docs: docs/DEV-MAIL-SERVER.md
        |  Ctrl+C to stop.
        |
        """.trimMargin(),
    )
}

class DevMailServer {
    private val mock = MailboxOrgMockServer(
        email = DevMailDefaults.EMAIL,
        password = DevMailDefaults.PASSWORD,
    )

    val email: String get() = mock.email
    val password: String get() = mock.password

    fun start() {
        mock.start(
            imapPort = DevMailDefaults.IMAP_PORT,
            smtpPort = DevMailDefaults.SMTP_PORT,
            httpPort = DevMailDefaults.HTTP_PORT,
        )
    }

    fun stop() = mock.stop()

    fun imapPort(): Int = mock.imapPort()
    fun smtpPort(): Int = mock.smtpPort()
}
