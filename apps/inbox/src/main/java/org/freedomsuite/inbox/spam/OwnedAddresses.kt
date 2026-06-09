package org.freedomsuite.inbox.spam

/**
 * Addresses and domains the user legitimately receives mail for.
 * Used to avoid false positives when mail is sent to an alias or secondary domain.
 */
data class OwnedAddresses(
    val addresses: Set<String>,
    val ownedDomains: Set<String>,
) {
    fun matches(candidate: String): Boolean {
        val normalized = normalizeEmail(candidate) ?: return false
        if (normalized in addresses) return true
        val domain = normalized.substringAfter('@', "")
        return domain.isNotEmpty() && domain in ownedDomains
    }

    companion object {
        fun fromAccount(
            primaryEmail: String,
            aliases: List<String> = emptyList(),
            ownedDomains: List<String> = emptyList(),
        ): OwnedAddresses {
            val primary = normalizeEmail(primaryEmail)
            val allAddresses = buildSet {
                primary?.let { add(it) }
                aliases.mapNotNull { normalizeEmail(it) }.forEach { add(it) }
            }
            val domains = buildSet {
                primary?.substringAfter('@')?.let { add(it) }
                ownedDomains.map { it.trim().lowercase().removePrefix("@") }
                    .filter { it.isNotEmpty() }
                    .forEach { add(it) }
            }
            return OwnedAddresses(addresses = allAddresses, ownedDomains = domains)
        }

        fun normalizeEmail(raw: String): String? {
            val trimmed = raw.trim().lowercase()
            if (trimmed.isEmpty()) return null
            val angle = Regex("""<([^>]+)>""").find(trimmed)?.groupValues?.get(1)
            val candidate = angle ?: trimmed
            if (!candidate.contains('@')) return null
            return candidate.trim()
        }

        fun extractAddresses(headerValue: String): List<String> =
            headerValue.split(',', ';')
                .mapNotNull { normalizeEmail(it) }
    }
}
