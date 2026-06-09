package org.freedomsuite.core.account.discovery

import org.freedomsuite.core.network.MailboxOrgDefaults

/**
 * Known provider endpoints keyed by email domain (exact match).
 */
object MailProviderPresets {
    fun forDomain(domain: String): MailServerSettings? {
        val key = domain.lowercase()
        return presets[key]
    }

    fun forEmail(email: String): MailServerSettings? {
        val domain = email.substringAfter('@', "").trim().lowercase()
        if (domain.isEmpty()) return null
        return forDomain(domain)
    }

    private val presets: Map<String, MailServerSettings> = mapOf(
        "mailbox.org" to settings(
            imap = MailboxOrgDefaults.IMAP_HOST,
            smtp = MailboxOrgDefaults.SMTP_HOST,
            smtpPort = MailboxOrgDefaults.SMTP_PORT_SSL,
            caldav = MailboxOrgDefaults.CALDAV_URL,
            label = "mailbox.org",
        ),
        "gmail.com" to settings("imap.gmail.com", "smtp.gmail.com", label = "Gmail"),
        "googlemail.com" to settings("imap.gmail.com", "smtp.gmail.com", label = "Gmail"),
        "outlook.com" to settings("outlook.office365.com", "smtp.office365.com", smtpPort = 587, label = "Outlook"),
        "hotmail.com" to settings("outlook.office365.com", "smtp.office365.com", smtpPort = 587, label = "Outlook"),
        "live.com" to settings("outlook.office365.com", "smtp.office365.com", smtpPort = 587, label = "Outlook"),
        "msn.com" to settings("outlook.office365.com", "smtp.office365.com", smtpPort = 587, label = "Outlook"),
        "office365.com" to settings("outlook.office365.com", "smtp.office365.com", smtpPort = 587, label = "Microsoft 365"),
        "fastmail.com" to settings("imap.fastmail.com", "smtp.fastmail.com", label = "Fastmail"),
        "fastmail.fm" to settings("imap.fastmail.com", "smtp.fastmail.com", label = "Fastmail"),
        "yahoo.com" to settings("imap.mail.yahoo.com", "smtp.mail.yahoo.com", label = "Yahoo Mail"),
        "icloud.com" to settings("imap.mail.me.com", "smtp.mail.me.com", smtpPort = 587, label = "iCloud"),
        "me.com" to settings("imap.mail.me.com", "smtp.mail.me.com", smtpPort = 587, label = "iCloud"),
        "mac.com" to settings("imap.mail.me.com", "smtp.mail.me.com", smtpPort = 587, label = "iCloud"),
        "zoho.com" to settings("imap.zoho.com", "smtp.zoho.com", label = "Zoho"),
        "migadu.com" to settings("imap.migadu.com", "smtp.migadu.com", label = "Migadu"),
        "freedom.test" to MailServerSettings(
            imapHost = DevMailServer.EMULATOR_HOST,
            imapPort = DevMailServer.IMAP_PORT,
            smtpHost = DevMailServer.EMULATOR_HOST,
            smtpPort = DevMailServer.SMTP_PORT,
            plainText = true,
            source = DiscoverySource.PRESET,
            label = "dev-mail-server",
        ),
    )

    private fun settings(
        imap: String,
        smtp: String,
        smtpPort: Int = 465,
        caldav: String? = null,
        label: String,
    ): MailServerSettings = MailServerSettings(
        imapHost = imap,
        imapPort = 993,
        smtpHost = smtp,
        smtpPort = smtpPort,
        caldavUrl = caldav,
        source = DiscoverySource.PRESET,
        label = label,
    )
}
