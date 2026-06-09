package org.freedomsuite.core.account.discovery

import android.content.Context
import java.net.InetAddress

/**
 * Domain-based host candidates. Tries common naming patterns; connection verification
 * in [org.freedomsuite.inbox.data.InboxRepository] picks the working endpoint.
 *
 * Full DNS MX/SRV parsing is deferred — Mozilla autoconfig + presets cover most providers.
 */
class MailDnsDiscovery(@Suppress("UNUSED_PARAMETER") context: Context) {
    suspend fun discover(domain: String): List<MailServerSettings> {
        val normalized = domain.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()

        val candidates = heuristicCandidates(normalized)
        return candidates.sortedByDescending { resolves(it.imapHost) }
    }

    private fun heuristicCandidates(domain: String): List<MailServerSettings> =
        listOf(
            pair("imap.$domain", "smtp.$domain"),
            pair("mail.$domain", "mail.$domain"),
            pair("imap.mail.$domain", "smtp.mail.$domain"),
            pair("mx.$domain", "smtp.$domain"),
        ).map { (imap, smtp) ->
            MailServerSettings(
                imapHost = imap,
                imapPort = 993,
                smtpHost = smtp,
                smtpPort = if (smtp == imap && imap.startsWith("mail.")) 587 else 465,
                source = DiscoverySource.HEURISTIC,
                label = "dns-heuristic",
            )
        }

    private fun pair(imap: String, smtp: String) = imap to smtp

    private fun resolves(host: String): Boolean = runCatching {
        InetAddress.getByName(host)
        true
    }.getOrDefault(false)
}
