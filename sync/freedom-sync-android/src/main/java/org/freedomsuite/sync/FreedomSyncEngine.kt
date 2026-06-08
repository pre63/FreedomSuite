package org.freedomsuite.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import org.freedomsuite.core.network.PrivacyHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.freedomsuite.core.crypto.FileEncryption
import org.freedomsuite.core.crypto.KeyDerivation
import org.freedomsuite.core.storage.EncryptedFileStore
import java.io.File
import java.security.SecureRandom

enum class SyncBackend {
    LOCAL_ONLY,
    MAILBOX_WEBDAV,
    S3_COMPATIBLE,
}

data class SyncConfig(
    val backend: SyncBackend = SyncBackend.LOCAL_ONLY,
    val webDavUrl: String? = null,
    val webDavEmail: String? = null,
    val webDavPassword: CharArray? = null,
    val s3Endpoint: String? = null,
    val s3Bucket: String? = null,
    val s3AccessKey: String? = null,
    val s3SecretKey: CharArray? = null,
)

data class SyncStatus(
    val lastSyncEpochMs: Long,
    val pendingUploads: Int,
    val pendingDownloads: Int,
    val message: String,
)

class FreedomSyncEngine(
    context: Context,
    private val config: SyncConfig,
    private val namespace: String = "default",
    private val backupFileName: String = "backup.bin",
    preferences: SyncPreferences? = null,
) {
    private val appContext = context.applicationContext
    private val prefs = preferences ?: AndroidSyncPreferences(appContext, "$PREFS_NAME.$namespace")
    private val client = PrivacyHttpClient.create(
        connectTimeoutSeconds = 30,
        readTimeoutSeconds = 60,
        writeTimeoutSeconds = 60,
    )

    suspend fun syncNow(payload: ByteArray, backupPassphrase: CharArray? = null): Result<SyncStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = encryptPayload(payload, backupPassphrase)
                when (config.backend) {
                    SyncBackend.LOCAL_ONLY -> uploadLocal(encrypted)
                    SyncBackend.MAILBOX_WEBDAV -> uploadWebDav(encrypted)
                    SyncBackend.S3_COMPATIBLE -> uploadS3(encrypted)
                }
                val now = System.currentTimeMillis()
                prefs.putLong(KEY_LAST_SYNC, now)
                SyncStatus(
                    lastSyncEpochMs = now,
                    pendingUploads = 0,
                    pendingDownloads = 0,
                    message = "Backup complete",
                )
            }
        }

    suspend fun downloadLatest(backupPassphrase: CharArray? = null): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = when (config.backend) {
                    SyncBackend.LOCAL_ONLY -> readLocal()
                    SyncBackend.MAILBOX_WEBDAV -> downloadWebDav()
                    SyncBackend.S3_COMPATIBLE -> downloadS3()
                }
                decryptPayload(encrypted, backupPassphrase)
            }
        }

    fun lastSyncEpochMs(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    private fun encryptPayload(payload: ByteArray, backupPassphrase: CharArray?): ByteArray {
        val key = encryptionKey(backupPassphrase)
        return FileEncryption.encrypt(key, payload)
    }

    private fun decryptPayload(encrypted: ByteArray, backupPassphrase: CharArray?): ByteArray {
        val key = encryptionKey(backupPassphrase)
        return FileEncryption.decrypt(key, encrypted)
    }

    private fun encryptionKey(backupPassphrase: CharArray?): ByteArray {
        return if (backupPassphrase != null) {
            deriveBackupKey(backupPassphrase)
        } else {
            deviceSyncKey()
        }
    }

    private fun uploadLocal(encrypted: ByteArray) {
        EncryptedFileStore.writeCiphertext(localBackupFile(), encrypted)
    }

    private fun readLocal(): ByteArray = EncryptedFileStore.readCiphertext(localBackupFile())

    private fun localBackupFile(): File =
        File(appContext.filesDir, "freedom-sync/$namespace/$backupFileName")

    private fun remotePath(): String = "freedom-sync/$namespace/$backupFileName"

    private fun uploadWebDav(encrypted: ByteArray) {
        val url = config.webDavUrl ?: error("WebDAV URL not configured")
        val email = config.webDavEmail ?: error("WebDAV email not configured")
        val password = config.webDavPassword ?: error("WebDAV password not configured")
        val request = Request.Builder()
            .url("${url.trimEnd('/')}/${remotePath()}")
            .header("Authorization", Credentials.basic(email, String(password)))
            .put(encrypted.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("WebDAV upload failed: ${response.code}")
    }

    private fun downloadWebDav(): ByteArray {
        val url = config.webDavUrl ?: error("WebDAV URL not configured")
        val email = config.webDavEmail ?: error("WebDAV email not configured")
        val password = config.webDavPassword ?: error("WebDAV password not configured")
        val request = Request.Builder()
            .url("${url.trimEnd('/')}/${remotePath()}")
            .header("Authorization", Credentials.basic(email, String(password)))
            .get()
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("WebDAV download failed: ${response.code}")
        return response.body?.bytes() ?: error("Empty WebDAV response")
    }

    private fun uploadS3(encrypted: ByteArray) {
        val endpoint = config.s3Endpoint ?: error("S3 endpoint not configured")
        val bucket = config.s3Bucket ?: error("S3 bucket not configured")
        val key = config.s3AccessKey ?: error("S3 access key not configured")
        val secret = config.s3SecretKey ?: error("S3 secret key not configured")
        val url = "${endpoint.trimEnd('/')}/$bucket/${remotePath()}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(key, String(secret)))
            .put(encrypted.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("S3 upload failed: ${response.code}")
    }

    private fun downloadS3(): ByteArray {
        val endpoint = config.s3Endpoint ?: error("S3 endpoint not configured")
        val bucket = config.s3Bucket ?: error("S3 bucket not configured")
        val key = config.s3AccessKey ?: error("S3 access key not configured")
        val secret = config.s3SecretKey ?: error("S3 secret key not configured")
        val url = "${endpoint.trimEnd('/')}/$bucket/${remotePath()}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(key, String(secret)))
            .get()
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("S3 download failed: ${response.code}")
        return response.body?.bytes() ?: error("Empty S3 response")
    }

    private fun deviceSyncKey(): ByteArray {
        val existing = prefs.getBytes(KEY_SYNC_KEY)
        if (existing != null) return existing
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.putBytes(KEY_SYNC_KEY, generated)
        return generated
    }

    companion object {
        private const val PREFS_NAME = "freedom_sync"
        private const val KEY_SYNC_KEY = "sync_key"
        private const val KEY_LAST_SYNC = "last_sync_epoch_ms"
        private val BACKUP_SALT = "freedom-suite-backup-v1".toByteArray()

        fun deriveBackupKey(passphrase: CharArray): ByteArray =
            KeyDerivation.pbkdf2Sha256(passphrase, BACKUP_SALT, iterations = 120_000)
    }
}
