package org.freedomsuite.inbox.spam

import org.freedomsuite.protocol.mime.MimeParser

/**
 * On-device rule-based spam scorer. See docs/SPAM-FILTER.md for rule definitions.
 */
class SpamClassifier(
    private val suspectThreshold: Int = 40,
    private val spamThreshold: Int = 70,
) {
    fun classify(input: SpamInput): SpamClassification {
        val hits = mutableListOf<SpamRuleHit>()
        val headers = input.headers

        if (input.hasCalendarInvite) {
            hits += SpamRuleHit("calendar_invite", -30, "Calendar invite")
        }

        if (isMailingList(headers)) {
            hits += SpamRuleHit("mailing_list", -20, "List-Id or list precedence")
        } else if (!isAddressedToUser(headers, input.owned)) {
            hits += SpamRuleHit("missing_recipient", 40, "Not addressed to any owned address")
        }

        authFailures(headers).forEach { detail ->
            hits += SpamRuleHit("auth_failure", 35, detail)
        }

        fromReplyMismatch(headers)?.let { detail ->
            hits += SpamRuleHit("from_reply_mismatch", 20, detail)
        }

        returnPathMismatch(headers)?.let { detail ->
            hits += SpamRuleHit("return_path_mismatch", 15, detail)
        }

        suspiciousSubject(input.subject)?.let { detail ->
            hits += SpamRuleHit("suspicious_subject", 15, detail)
        }

        if (input.subject.isBlank() || input.subject.equals("(no subject)", ignoreCase = true)) {
            hits += SpamRuleHit("empty_subject", 5, "Missing subject")
        }

        urlCount(input.body).let { count ->
            if (count > 5) hits += SpamRuleHit("url_heavy", 10, "$count links")
        }

        suspiciousLinkTlds(input.body)?.let { detail ->
            hits += SpamRuleHit("suspicious_tld", 15, detail)
        }

        displayNameSpoof(input.from, headers)?.let { detail ->
            hits += SpamRuleHit("display_name_spoof", 25, detail)
        }

        providerSpamScore(headers)?.let { detail ->
            hits += SpamRuleHit("provider_spam_header", 50, detail)
        }

        if (input.from.equals("unknown", ignoreCase = true) || input.from.contains('@').not()) {
            hits += SpamRuleHit("suspicious_from", 10, "Unparseable From")
        }

        val score = hits.sumOf { it.weight }.coerceAtLeast(0)
        val verdict = when {
            score >= spamThreshold -> SpamVerdict.SPAM
            score >= suspectThreshold -> SpamVerdict.SUSPECT
            else -> SpamVerdict.HAM
        }
        return SpamClassification(score = score, verdict = verdict, hits = hits)
    }

    private fun isAddressedToUser(headers: Map<String, List<String>>, owned: OwnedAddresses): Boolean {
        val recipientHeaders = listOf(
            "To", "Cc", "Delivered-To", "X-Original-To", "Envelope-To", "X-Envelope-To",
        )
        val recipients = recipientHeaders.flatMap { name ->
            MimeParser.headerValues(headers, name).flatMap { OwnedAddresses.extractAddresses(it) }
        }
        return recipients.any { owned.matches(it) }
    }

    private fun isMailingList(headers: Map<String, List<String>>): Boolean {
        if (MimeParser.headerValues(headers, "List-Id").isNotEmpty()) return true
        val precedence = MimeParser.headerValues(headers, "Precedence").firstOrNull()?.lowercase()
        return precedence in listOf("list", "bulk", "junk")
    }

    private fun authFailures(headers: Map<String, List<String>>): List<String> {
        val auth = MimeParser.headerValues(headers, "Authentication-Results") +
            MimeParser.headerValues(headers, "ARC-Authentication-Results")
        if (auth.isEmpty()) return emptyList()
        val combined = auth.joinToString(" ").lowercase()
        val failures = mutableListOf<String>()
        if (combined.contains("spf=fail") || combined.contains("spf=softfail")) failures += "SPF fail"
        if (combined.contains("dkim=fail")) failures += "DKIM fail"
        if (combined.contains("dmarc=fail")) failures += "DMARC fail"
        return failures
    }

    private fun fromReplyMismatch(headers: Map<String, List<String>>): String? {
        val from = MimeParser.headerValues(headers, "From").firstOrNull() ?: return null
        val replyTo = MimeParser.headerValues(headers, "Reply-To").firstOrNull() ?: return null
        val fromDomain = domainOf(from) ?: return null
        val replyDomain = domainOf(replyTo) ?: return null
        return if (fromDomain != replyDomain) "$fromDomain vs $replyDomain" else null
    }

    private fun returnPathMismatch(headers: Map<String, List<String>>): String? {
        val from = MimeParser.headerValues(headers, "From").firstOrNull() ?: return null
        val returnPath = MimeParser.headerValues(headers, "Return-Path").firstOrNull() ?: return null
        val fromDomain = domainOf(from) ?: return null
        val returnDomain = domainOf(returnPath) ?: return null
        return if (fromDomain != returnDomain) "$returnDomain vs $fromDomain" else null
    }

    private fun suspiciousSubject(subject: String): String? {
        val lower = subject.lowercase()
        val patterns = listOf(
            "viagra", "cialis", "lottery", "winner", "bitcoin giveaway", "crypto giveaway",
            "nigerian prince", "wire transfer", "account suspended", "verify your account",
            "act now", "limited time offer", "weight loss", "work from home",
        )
        patterns.firstOrNull { lower.contains(it) }?.let { return "Keyword: $it" }
        if (subject.length > 8 && subject == subject.uppercase() && subject.any { it.isLetter() }) {
            return "All-caps subject"
        }
        if (subject.count { it == '!' } >= 3) return "Excessive punctuation"
        return null
    }

    private fun urlCount(body: String): Int =
        Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE).findAll(body).count()

    private fun suspiciousLinkTlds(body: String): String? {
        val suspicious = setOf(".xyz", ".top", ".click", ".loan", ".work", ".gq", ".tk", ".ml", ".cf")
        val tld = suspicious.firstOrNull { tld -> body.lowercase().contains(tld) }
        return tld?.let { "Link TLD $it" }
    }

    private fun displayNameSpoof(from: String, headers: Map<String, List<String>>): String? {
        val fromHeader = MimeParser.headerValues(headers, "From").firstOrNull() ?: from
        val display = fromHeader.substringBefore('<').trim().lowercase()
        val brands = listOf("paypal", "amazon", "apple", "microsoft", "google", "bank", "netflix", "dhl", "ups")
        val brand = brands.firstOrNull { display.contains(it) } ?: return null
        val fromDomain = domainOf(fromHeader) ?: return null
        if (!fromDomain.contains(brand.replace(" ", ""))) {
            return "Brand $brand from $fromDomain"
        }
        return null
    }

    private fun providerSpamScore(headers: Map<String, List<String>>): String? {
        val flag = MimeParser.headerValues(headers, "X-Spam-Flag").firstOrNull()
        if (flag.equals("YES", ignoreCase = true)) return "X-Spam-Flag YES"
        val status = MimeParser.headerValues(headers, "X-Spam-Status").firstOrNull()
        if (status?.lowercase()?.contains("yes") == true) return status
        val scoreHeader = MimeParser.headerValues(headers, "X-Spam-Score").firstOrNull()
            ?: MimeParser.headerValues(headers, "X-Spam-Level").firstOrNull()
        val score = scoreHeader?.trim()?.substringBefore(' ')?.toDoubleOrNull()
        if (score != null && score >= 5.0) return "Score $score"
        return null
    }

    private fun domainOf(raw: String): String? {
        val email = OwnedAddresses.normalizeEmail(raw) ?: return null
        return email.substringAfter('@').takeIf { it.isNotEmpty() }
    }
}

data class SpamInput(
    val subject: String,
    val from: String,
    val body: String,
    val headers: Map<String, List<String>>,
    val owned: OwnedAddresses,
    val hasCalendarInvite: Boolean = false,
)
