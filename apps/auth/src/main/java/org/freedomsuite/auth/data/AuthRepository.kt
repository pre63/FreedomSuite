package org.freedomsuite.auth.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.freedomsuite.core.crypto.SecurePreferences
import org.freedomsuite.core.totp.OtpAuthAccount
import org.freedomsuite.core.totp.OtpAuthParser
import org.freedomsuite.core.totp.TotpAlgorithm
import org.freedomsuite.core.totp.TotpGenerator
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class AuthCode(
    val account: AuthAccountEntity,
    val code: String,
    val secondsRemaining: Int,
)

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AuthDatabase.getInstance(appContext).authDao()
    private val syncPrefs = SecurePreferences(appContext, "freedom_auth_sync")

    fun observeAccounts(): Flow<List<AuthAccountEntity>> = dao.observeAll()

    fun currentCodes(accounts: List<AuthAccountEntity>, nowMs: Long = System.currentTimeMillis()): List<AuthCode> {
        return accounts.map { account ->
            val secret = org.freedomsuite.core.totp.Base32.decode(account.secretBase32)
            val algorithm = TotpAlgorithm.fromName(account.algorithm)
            AuthCode(
                account = account,
                code = TotpGenerator.generate(
                    secret = secret,
                    algorithm = algorithm,
                    digits = account.digits,
                    periodSeconds = account.periodSeconds,
                    timestampMs = nowMs,
                ),
                secondsRemaining = TotpGenerator.secondsRemaining(account.periodSeconds, nowMs),
            )
        }
    }

    suspend fun addFromOtpAuth(uri: String): Result<AuthAccountEntity> = runCatching {
        val parsed = OtpAuthParser.parse(uri).getOrThrow()
        addAccount(parsed)
    }

    suspend fun addManual(
        issuer: String,
        accountName: String,
        secretBase32: String,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
        digits: Int = 6,
        periodSeconds: Int = 30,
    ): Result<AuthAccountEntity> = runCatching {
        val normalized = OtpAuthParser.normalizeSecret(secretBase32)
        addAccount(
            OtpAuthAccount(
                issuer = issuer.trim(),
                accountName = accountName.trim(),
                secretBase32 = normalized,
                algorithm = algorithm,
                digits = digits,
                periodSeconds = periodSeconds,
            ),
        )
    }

    private suspend fun addAccount(parsed: OtpAuthAccount): AuthAccountEntity {
        require(parsed.issuer.isNotBlank()) { "Issuer required" }
        require(parsed.accountName.isNotBlank()) { "Account name required" }
        val existing = dao.getAll()
        val entity = AuthAccountEntity(
            id = UUID.randomUUID().toString(),
            issuer = parsed.issuer,
            accountName = parsed.accountName,
            secretBase32 = parsed.secretBase32,
            algorithm = parsed.algorithm.name,
            digits = parsed.digits,
            periodSeconds = parsed.periodSeconds,
            createdAtEpochMs = System.currentTimeMillis(),
            sortOrder = existing.size,
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun deleteAccount(id: String) {
        dao.deleteById(id)
    }

    suspend fun backupNow(backupPassphrase: CharArray): Result<String> {
        val engine = syncEngine()
        val payload = exportSnapshot().toByteArray(Charsets.UTF_8)
        return engine.syncNow(payload, backupPassphrase).map { it.message }
    }

    suspend fun restoreFromBackup(backupPassphrase: CharArray): Result<Int> {
        val engine = syncEngine()
        val payload = engine.downloadLatest(backupPassphrase).getOrThrow()
        return importSnapshot(String(payload, Charsets.UTF_8))
    }

    fun lastBackupEpochMs(): Long = syncEngine().lastSyncEpochMs()

    fun isBackupEnabled(): Boolean = syncPrefs.getBoolean(KEY_BACKUP_ENABLED, false)

    fun setBackupEnabled(enabled: Boolean) {
        syncPrefs.putBoolean(KEY_BACKUP_ENABLED, enabled)
    }

    fun hasBackupPassphraseConfigured(): Boolean =
        syncPrefs.getString(KEY_BACKUP_PASSPHRASE_HINT) != null

    fun saveBackupPassphraseHint(passphrase: CharArray) {
        syncPrefs.putString(KEY_BACKUP_PASSPHRASE_HINT, "configured")
        syncPrefs.putString(KEY_BACKUP_PASSPHRASE_CHECK, hashPassphrase(passphrase))
    }

    fun verifyBackupPassphrase(passphrase: CharArray): Boolean {
        val stored = syncPrefs.getString(KEY_BACKUP_PASSPHRASE_CHECK) ?: return false
        return stored == hashPassphrase(passphrase)
    }

    fun saveSyncConfig(config: SyncConfig) {
        syncPrefs.putString(KEY_BACKEND, config.backend.name)
        config.webDavUrl?.let { syncPrefs.putString(KEY_WEBDAV_URL, it) }
        config.webDavEmail?.let { syncPrefs.putString(KEY_WEBDAV_EMAIL, it) }
        config.webDavPassword?.let { syncPrefs.putString(KEY_WEBDAV_PASSWORD, String(it)) }
        config.s3Endpoint?.let { syncPrefs.putString(KEY_S3_ENDPOINT, it) }
        config.s3Bucket?.let { syncPrefs.putString(KEY_S3_BUCKET, it) }
        config.s3AccessKey?.let { syncPrefs.putString(KEY_S3_ACCESS_KEY, it) }
        config.s3SecretKey?.let { syncPrefs.putString(KEY_S3_SECRET_KEY, String(it)) }
    }

    fun loadSyncConfig(): SyncConfig = SyncConfig(
        backend = runCatching {
            SyncBackend.valueOf(syncPrefs.getString(KEY_BACKEND) ?: SyncBackend.LOCAL_ONLY.name)
        }.getOrDefault(SyncBackend.LOCAL_ONLY),
        webDavUrl = syncPrefs.getString(KEY_WEBDAV_URL),
        webDavEmail = syncPrefs.getString(KEY_WEBDAV_EMAIL),
        webDavPassword = syncPrefs.getString(KEY_WEBDAV_PASSWORD)?.toCharArray(),
        s3Endpoint = syncPrefs.getString(KEY_S3_ENDPOINT),
        s3Bucket = syncPrefs.getString(KEY_S3_BUCKET),
        s3AccessKey = syncPrefs.getString(KEY_S3_ACCESS_KEY),
        s3SecretKey = syncPrefs.getString(KEY_S3_SECRET_KEY)?.toCharArray(),
    )

    private fun syncEngine(): FreedomSyncEngine =
        FreedomSyncEngine(appContext, loadSyncConfig(), namespace = "auth", backupFileName = "auth.bin")

    private suspend fun exportSnapshot(): String {
        val accounts = dao.getAll()
        val root = JSONObject()
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("id", account.id)
                    .put("issuer", account.issuer)
                    .put("accountName", account.accountName)
                    .put("secretBase32", account.secretBase32)
                    .put("algorithm", account.algorithm)
                    .put("digits", account.digits)
                    .put("periodSeconds", account.periodSeconds)
                    .put("createdAtEpochMs", account.createdAtEpochMs)
                    .put("sortOrder", account.sortOrder),
            )
        }
        root.put("version", 1)
        root.put("accounts", array)
        return root.toString()
    }

    private suspend fun importSnapshot(json: String): Result<Int> {
        return runCatching {
            val root = JSONObject(json)
            val array = root.getJSONArray("accounts")
            val imported = mutableListOf<AuthAccountEntity>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                imported += AuthAccountEntity(
                    id = item.getString("id"),
                    issuer = item.getString("issuer"),
                    accountName = item.getString("accountName"),
                    secretBase32 = item.getString("secretBase32"),
                    algorithm = item.optString("algorithm", TotpAlgorithm.SHA1.name),
                    digits = item.optInt("digits", 6),
                    periodSeconds = item.optInt("periodSeconds", 30),
                    createdAtEpochMs = item.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    sortOrder = item.optInt("sortOrder", i),
                )
            }
            dao.upsertAll(imported)
            imported.size
        }
    }

    private fun hashPassphrase(passphrase: CharArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(String(passphrase).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_BACKUP_ENABLED = "backup_enabled"
        private const val KEY_BACKUP_PASSPHRASE_HINT = "backup_passphrase_hint"
        private const val KEY_BACKUP_PASSPHRASE_CHECK = "backup_passphrase_check"
        private const val KEY_BACKEND = "backend"
        private const val KEY_WEBDAV_URL = "webdav_url"
        private const val KEY_WEBDAV_EMAIL = "webdav_email"
        private const val KEY_WEBDAV_PASSWORD = "webdav_password"
        private const val KEY_S3_ENDPOINT = "s3_endpoint"
        private const val KEY_S3_BUCKET = "s3_bucket"
        private const val KEY_S3_ACCESS_KEY = "s3_access_key"
        private const val KEY_S3_SECRET_KEY = "s3_secret_key"
    }
}
