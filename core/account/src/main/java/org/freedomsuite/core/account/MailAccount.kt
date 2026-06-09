package org.freedomsuite.core.account

/**
 * Registered send identity (primary, alias, alternative sender).
 */
data class EmailIdentity(
    val address: String,
    val label: String,
    val isDefault: Boolean = false,
)

/**
 * Mail account configuration (IMAP/SMTP endpoints discovered or entered manually).
 */
data class MailAccount(
    val email: String,
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int,
    val caldavUrl: String = org.freedomsuite.core.network.MailboxOrgDefaults.CALDAV_URL,
    val plainText: Boolean = false,
    /** Additional receive addresses (aliases on any owned domain). */
    val aliases: List<String> = emptyList(),
    /** Domains where any `user@domain` is a valid owned address (multi-domain mailboxes). */
    val ownedDomains: List<String> = emptyList(),
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
