package org.freedomsuite.testing.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreedomSyncIntegrationTest : IntegrationTestBase() {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun webDavBackupRoundTrip() = runBlocking {
        val passphrase = "integration-backup-pass".toCharArray()
        val payload = """{"accounts":[{"issuer":"GitHub","secret":"JBSWY3DPEHPK3PXP"}]}""".toByteArray()
        val config = SyncConfig(
            backend = SyncBackend.MAILBOX_WEBDAV,
            webDavUrl = mock.webDavUrl(),
            webDavEmail = mock.email,
            webDavPassword = testPassword(),
        )
        val engine = FreedomSyncEngine(
            context,
            config,
            namespace = "integration-test",
            backupFileName = "test.bin",
            preferences = InMemorySyncPreferences(),
        )

        engine.syncNow(payload, passphrase).getOrThrow()
        val restored = engine.downloadLatest(passphrase).getOrThrow()

        assertArrayEquals(payload, restored)
    }

    @Test
    fun s3CompatibleBackupRoundTrip() = runBlocking {
        val passphrase = "s3-backup-passphrase".toCharArray()
        val payload = "encrypted-blob-payload".toByteArray()
        val config = SyncConfig(
            backend = SyncBackend.S3_COMPATIBLE,
            s3Endpoint = mock.s3Endpoint(),
            s3Bucket = mock.s3Bucket(),
            s3AccessKey = "test-key",
            s3SecretKey = "test-secret".toCharArray(),
        )
        val engine = FreedomSyncEngine(
            context,
            config,
            namespace = "s3-test",
            backupFileName = "blob.bin",
            preferences = InMemorySyncPreferences(),
        )

        val status = engine.syncNow(payload, passphrase).getOrThrow()
        assertTrue(status.lastSyncEpochMs > 0)

        val restored = engine.downloadLatest(passphrase).getOrThrow()
        assertArrayEquals(payload, restored)
    }

    @Test
    fun localBackupRoundTrip() = runBlocking {
        val payload = """{"version":1}""".toByteArray()
        val engine = FreedomSyncEngine(
            context,
            SyncConfig(backend = SyncBackend.LOCAL_ONLY),
            namespace = "local-test",
            backupFileName = "local.bin",
            preferences = InMemorySyncPreferences(),
        )

        engine.syncNow(payload).getOrThrow()
        val restored = engine.downloadLatest().getOrThrow()
        assertArrayEquals(payload, restored)
    }
}
