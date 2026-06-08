package org.freedomsuite.core.network

/**
 * mailbox.org and generic provider defaults.
 */
object MailboxOrgDefaults {
    const val IMAP_HOST = "imap.mailbox.org"
    const val IMAP_PORT = 993
    const val SMTP_HOST = "smtp.mailbox.org"
    const val SMTP_PORT_SSL = 465
    const val SMTP_PORT_STARTTLS = 587
    const val CALDAV_URL = "https://dav.mailbox.org"
    const val WEBDAV_URL = "https://dav.mailbox.org"
}

/**
 * Returns true if the email domain is mailbox.org or a known mailbox.org custom domain pattern.
 * Custom domains require manual server entry.
 */
fun isMailboxOrgDomain(email: String): Boolean {
    val domain = email.substringAfter('@', "").lowercase()
    return domain == "mailbox.org" || domain.endsWith(".mailbox.org")
}
