package org.freedomsuite.core.account.discovery

import android.content.Context

/**
 * Discovers IMAP/SMTP settings for an email domain using presets, autoconfig, DNS, and heuristics.
 */
class MailServerDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val autoconfig = MailAutoconfigDiscovery()
    private val dns = MailDnsDiscovery(appContext)

    suspend fun discover(email: String): List<MailServerSettings> {
        val domain = email.substringAfter('@', "").trim().lowercase()
        if (domain.isEmpty()) return emptyList()

        val ordered = linkedSetOf<MailServerSettings>()

        MailProviderPresets.forDomain(domain)?.let { ordered += it }

        runCatching { autoconfig.discover(domain) }.getOrNull()?.let { ordered += it }

        runCatching { dns.discover(domain) }.getOrDefault(emptyList()).forEach { ordered += it }

        return ordered.toList()
    }
}
