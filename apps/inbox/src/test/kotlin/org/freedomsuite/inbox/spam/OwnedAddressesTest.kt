package org.freedomsuite.inbox.spam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnedAddressesTest {
    @Test
    fun matchesPrimaryAliasAndOwnedDomains() {
        val owned = OwnedAddresses.fromAccount(
            primaryEmail = "Me@Primary.COM",
            aliases = listOf("Alias@Second.org"),
            ownedDomains = listOf("second.org", "third.net"),
        )
        assertTrue(owned.matches("me@primary.com"))
        assertTrue(owned.matches("Alias Person <alias@second.org>"))
        assertTrue(owned.matches("anything@third.net"))
        assertFalse(owned.matches("other@example.com"))
    }
}
