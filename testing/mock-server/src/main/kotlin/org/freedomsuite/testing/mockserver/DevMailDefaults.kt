package org.freedomsuite.testing.mockserver

/**
 * Fixed ports and credentials for the local dev IMAP/SMTP server.
 * Keep in sync with [org.freedomsuite.core.account.discovery.DevMailServer] in core:account.
 */
object DevMailDefaults {
    const val EMAIL = "test@freedom.test"
    const val PASSWORD = "dev-password"
    const val IMAP_PORT = 1143
    const val SMTP_PORT = 1025
    const val HTTP_PORT = 18080
}
