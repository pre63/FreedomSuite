package org.freedomsuite.core.account.discovery

import org.freedomsuite.core.account.MailAccount

enum class DiscoverySource {
    PRESET,
    AUTOCONFIG,
    DNS_SRV,
    DNS_MX,
    HEURISTIC,
    MANUAL,
}

data class MailServerSettings(
    val imapHost: String,
    val imapPort: Int = 993,
    val smtpHost: String,
    val smtpPort: Int = 465,
    val caldavUrl: String? = null,
    val plainText: Boolean = false,
    val source: DiscoverySource,
    val label: String = source.name.lowercase(),
) {
    fun toMailAccount(email: String): MailAccount = MailAccount(
        email = email.trim(),
        imapHost = imapHost,
        imapPort = imapPort,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        caldavUrl = caldavUrl.orEmpty(),
        plainText = plainText,
    )
}
