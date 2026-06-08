package org.freedomsuite.testing.integration

import org.freedomsuite.core.crypto.CryptoAlgorithms
import org.freedomsuite.core.crypto.FileEncryption
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.SecureRandom

class FileEncryptionTest {
    @Test
    fun encryptsWithVersionByte() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = "secret payload".toByteArray()
        val encrypted = FileEncryption.encrypt(key, plaintext)
        assertEquals(CryptoAlgorithms.VERSION.toByte(), encrypted[0])
        assertArrayEquals(plaintext, FileEncryption.decrypt(key, encrypted))
    }

    @Test
    fun readsLegacyBlobsWithoutVersionByte() {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val plaintext = "legacy".toByteArray()
        val withVersion = FileEncryption.encrypt(key, plaintext)
        val legacy = withVersion.copyOfRange(1, withVersion.size)
        assertArrayEquals(plaintext, FileEncryption.decrypt(key, legacy))
    }
}
