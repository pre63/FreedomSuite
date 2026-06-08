package org.freedomsuite.core.account

/**
 * Registered send identity for mailbox.org (primary, alias, alternative sender).
 */
data class EmailIdentity(
    val address: String,
    val label: String,
    val isDefault: Boolean = false,
)

/**
 * mailbox.org mail account configuration.
 */
data class MailAccount(
    val email: String,
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int,
    val caldavUrl: String = org.freedomsuite.core.network.MailboxOrgDefaults.CALDAV_URL,
    val identities: List<EmailIdentity> = emptyList(),
) {
    companion object {
        fun mailboxOrg(email: String): MailAccount = MailAccount(
            email = email,
            imapHost = org.freedomsuite.core.network.MailboxOrgDefaults.IMAP_HOST,
            imapPort = org.freedomsuite.core.network.MailboxOrgDefaults.IMAP_PORT,
            smtpHost = org.freedomsuite.core.network.MailboxOrgDefaults.SMTP_HOST,
            smtpPort = org.freedomsuite.core.network.MailboxOrgDefaults.SMTP_PORT_SSL,
            caldavUrl = org.freedomsuite.core.network.MailboxOrgDefaults.CALDAV_URL,
            identities = listOf(EmailIdentity(email, "Primary", isDefault = true)),
        )
    }
}
