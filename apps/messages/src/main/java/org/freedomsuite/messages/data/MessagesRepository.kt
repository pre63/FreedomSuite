package org.freedomsuite.messages.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.freedomsuite.core.crypto.SecurePreferences
import org.freedomsuite.core.storage.EncryptedFileStore
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class MessagesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val channelDao = MessagesDatabase.getInstance(appContext).channelDao()
    private val messageDao = MessagesDatabase.getInstance(appContext).messageDao()
    private val syncPrefs = SecurePreferences(appContext, "freedom_messages_sync")
    private val fileKey = MessagesKeyManager(appContext).getOrCreateAttachmentKey()

    fun observeChannels(): Flow<List<ChannelWithPreview>> =
        channelDao.observeAllWithPreview().map { rows ->
            rows.map { it.toChannelWithPreview() }
        }

    fun observeMessages(channelId: String): Flow<List<MessageEntity>> =
        messageDao.observeForChannel(channelId)

    suspend fun ensureDefaultPersonalChannel() {
        val existing = channelDao.getById(DEFAULT_PERSONAL_CHANNEL_ID)
        if (existing == null) {
            channelDao.insert(
                ChannelEntity(
                    id = DEFAULT_PERSONAL_CHANNEL_ID,
                    name = "My Channel",
                    type = ChannelType.PERSONAL.name,
                    createdAtEpochMs = System.currentTimeMillis(),
                    memberCount = 1,
                ),
            )
        }
    }

    suspend fun createChannel(name: String, type: ChannelType): ChannelEntity {
        val channel = ChannelEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type.name,
            createdAtEpochMs = System.currentTimeMillis(),
            memberCount = if (type == ChannelType.PERSONAL) 1 else 1,
        )
        channelDao.insert(channel)
        return channel
    }

    suspend fun deleteChannel(channelId: String) {
        if (channelId == DEFAULT_PERSONAL_CHANNEL_ID) return
        channelDao.deleteById(channelId)
    }

    suspend fun getChannel(id: String): ChannelEntity? = channelDao.getById(id)

    suspend fun postMessage(channelId: String, body: String) {
        val trimmed = body.trim()
        require(trimmed.isNotEmpty())
        messageDao.insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                body = trimmed,
                createdAtEpochMs = System.currentTimeMillis(),
                authorLabel = "You",
            ),
        )
    }

    suspend fun postPhoto(channelId: String, uri: Uri, mimeType: String) {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image")
        val file = File(appContext.filesDir, "messages_attachments/${UUID.randomUUID()}.bin")
        EncryptedFileStore.writeEncrypted(fileKey, file, bytes)
        messageDao.insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                body = "📷 Photo",
                createdAtEpochMs = System.currentTimeMillis(),
                authorLabel = "You",
                attachmentPath = file.absolutePath,
                attachmentMimeType = mimeType,
            ),
        )
    }

    fun readAttachment(path: String): ByteArray {
        return EncryptedFileStore.readEncrypted(fileKey, File(path))
    }

    suspend fun syncNow(): Result<String> {
        val engine = syncEngine()
        val payload = exportSnapshot().toByteArray()
        return engine.syncNow(payload).map { it.message }
    }

    suspend fun restoreFromSync(): Result<Int> {
        val engine = syncEngine()
        val payload = engine.downloadLatest().getOrThrow()
        return importSnapshot(String(payload))
    }

    fun lastSyncEpochMs(): Long {
        val engine = syncEngine()
        return engine.lastSyncEpochMs()
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

    private fun syncEngine(): FreedomSyncEngine =
        FreedomSyncEngine(appContext, loadSyncConfig(), namespace = "messages", backupFileName = "messages.bin")

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

    private suspend fun exportSnapshot(): String {
        val channels = channelDao.getAll()
        val messages = messageDao.getAll()
        val root = JSONObject()
        val channelArray = JSONArray()
        channels.forEach { channel ->
            channelArray.put(
                JSONObject()
                    .put("id", channel.id)
                    .put("name", channel.name)
                    .put("type", channel.type)
                    .put("createdAtEpochMs", channel.createdAtEpochMs)
                    .put("memberCount", channel.memberCount),
            )
        }
        val messageArray = JSONArray()
        messages.forEach { message ->
            messageArray.put(
                JSONObject()
                    .put("id", message.id)
                    .put("channelId", message.channelId)
                    .put("body", message.body)
                    .put("createdAtEpochMs", message.createdAtEpochMs)
                    .put("authorLabel", message.authorLabel)
                    .put("attachmentPath", message.attachmentPath)
                    .put("attachmentMimeType", message.attachmentMimeType),
            )
        }
        root.put("channels", channelArray)
        root.put("messages", messageArray)
        return root.toString()
    }

    private suspend fun importSnapshot(json: String): Result<Int> {
        return runCatching {
            val root = JSONObject(json)
            val channels = root.getJSONArray("channels")
            val messages = root.getJSONArray("messages")
            for (i in 0 until channels.length()) {
                val item = channels.getJSONObject(i)
                channelDao.insert(
                    ChannelEntity(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        type = item.getString("type"),
                        createdAtEpochMs = item.getLong("createdAtEpochMs"),
                        memberCount = item.getInt("memberCount"),
                    ),
                )
            }
            for (i in 0 until messages.length()) {
                val item = messages.getJSONObject(i)
                messageDao.insert(
                    MessageEntity(
                        id = item.getString("id"),
                        channelId = item.getString("channelId"),
                        body = item.getString("body"),
                        createdAtEpochMs = item.getLong("createdAtEpochMs"),
                        authorLabel = item.optString("authorLabel", "You"),
                        attachmentPath = item.optString("attachmentPath").ifBlank { null },
                        attachmentMimeType = item.optString("attachmentMimeType").ifBlank { null },
                    ),
                )
            }
            messages.length()
        }
    }

    companion object {
        const val DEFAULT_PERSONAL_CHANNEL_ID = "personal-default"
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

