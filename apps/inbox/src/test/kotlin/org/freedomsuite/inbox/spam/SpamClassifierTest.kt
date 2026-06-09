package org.freedomsuite.inbox.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamClassifierTest {
    private val classifier = SpamClassifier()
    private val owned = OwnedAddresses.fromAccount(
        primaryEmail = "me@primary.com",
        aliases = listOf("shop@other.org", "news@primary.com"),
        ownedDomains = listOf("primary.com", "other.org"),
    )

    @Test
    fun aliasInTo_isHam() {
        val result = classify(
            to = "shop@other.org",
            subject = "Order shipped",
            from = "store@retailer.com",
        )
        assertEquals(SpamVerdict.HAM, result.verdict)
    }

    @Test
    fun anyAddressOnOwnedDomain_isHam() {
        val result = classify(
            to = "random-alias@other.org",
            subject = "Hello",
            from = "friend@example.net",
        )
        assertEquals(SpamVerdict.HAM, result.verdict)
    }

    @Test
    fun missingRecipient_isSuspectOrSpam() {
        val result = classify(
            to = "stranger@notmine.com",
            subject = "You won a prize!!!",
            from = "promo@scam.xyz",
            body = "Click https://evil.xyz/now https://evil.xyz/two",
        )
        assertTrue(result.verdict != SpamVerdict.HAM)
        assertTrue(result.hits.any { it.ruleId == "missing_recipient" })
    }

    @Test
    fun mailingListWithoutUserInTo_isHam() {
        val result = classify(
            to = "list@freelists.org",
            subject = "Weekly digest",
            from = "digest@freelists.org",
            extraHeaders = mapOf("List-Id" to listOf("<weekly.example.com>")),
        )
        assertEquals(SpamVerdict.HAM, result.verdict)
    }

    @Test
    fun authFailure_increasesScore() {
        val result = classify(
            to = "me@primary.com",
            subject = "Hello",
            from = "fake@bank.com",
            extraHeaders = mapOf(
                "Authentication-Results" to listOf("spf=fail dkim=fail dmarc=fail"),
            ),
        )
        assertTrue(result.hits.any { it.ruleId == "auth_failure" })
    }

    @Test
    fun calendarInvite_reducesSpamScore() {
        val result = classify(
            to = "me@primary.com",
            subject = "Meeting invite",
            from = "colleague@work.com",
            hasCalendarInvite = true,
        )
        assertTrue(result.hits.any { it.weight < 0 })
    }

    private fun classify(
        to: String,
        subject: String,
        from: String,
        body: String = "",
        extraHeaders: Map<String, List<String>> = emptyMap(),
        hasCalendarInvite: Boolean = false,
    ): SpamClassification {
        val headers = linkedMapOf(
            "To" to listOf(to),
            "From" to listOf(from),
        )
        headers.putAll(extraHeaders)
        return classifier.classify(
            SpamInput(
                subject = subject,
                from = from,
                body = body,
                headers = headers,
                owned = owned,
                hasCalendarInvite = hasCalendarInvite,
            ),
        )
    }
}
