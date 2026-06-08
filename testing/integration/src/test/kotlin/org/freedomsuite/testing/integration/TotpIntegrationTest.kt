package org.freedomsuite.testing.integration

import org.freedomsuite.core.totp.Base32
import org.freedomsuite.core.totp.OtpAuthParser
import org.freedomsuite.core.totp.TotpAlgorithm
import org.freedomsuite.core.totp.TotpGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpIntegrationTest {
    @Test
    fun totpIsDeterministicForTimestamp() {
        val secret = Base32.decode("JBSWY3DPEHPK3PXP")
        val timestamp = 1_700_000_000_000L
        val first = TotpGenerator.generate(
            secret = secret,
            algorithm = TotpAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            timestampMs = timestamp,
        )
        val second = TotpGenerator.generate(
            secret = secret,
            algorithm = TotpAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            timestampMs = timestamp,
        )
        assertEquals(first, second)
        assertEquals(6, first.length)
        assertTrue(first.all { it.isDigit() })
    }

    @Test
    fun parseOtpAuthUri() {
        val uri = "otpauth://totp/GitHub:user@mailbox.org?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30"
        val account = OtpAuthParser.parse(uri).getOrThrow()
        assertEquals("GitHub", account.issuer)
        assertEquals("user@mailbox.org", account.accountName)
        assertEquals("JBSWY3DPEHPK3PXP", account.secretBase32)
    }
}
