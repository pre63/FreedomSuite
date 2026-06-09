package org.freedomsuite.files.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.flow.Flow
import org.freedomsuite.core.crypto.SecurePreferences
import org.freedomsuite.core.storage.EncryptedFileStore
import org.freedomsuite.core.vision.EmbeddingCodec
import org.freedomsuite.core.vision.FaceMatcher
import org.freedomsuite.core.vision.LocalVisionEngine
import org.freedomsuite.core.vision.VisionSearch
import org.freedomsuite.sync.FreedomSyncEngine
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import java.io.File
import java.util.Base64
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class FilesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = FilesDatabase.getInstance(appContext).filesDao()
    private val fileKey = FilesKeyManager(appContext).getOrCreateFileKey()
    private val syncPrefs = SecurePreferences(appContext, "freedom_files_sync")

    fun observeFolders(): Flow<List<FolderWithCount>> = dao.observeFoldersWithCount()

    fun observeFiles(folderId: String): Flow<List<FileItemEntity>> =
        dao.observeFilesInFolder(folderId)

    suspend fun ensureDefaultFolders() {
        val now = System.currentTimeMillis()
        if (dao.getFolder(DEFAULT_PHOTOS_ID) == null) {
            dao.upsertFolder(
                FolderEntity(
                    id = DEFAULT_PHOTOS_ID,
                    name = "Photos",
                    kind = FolderKind.PHOTOS.name,
                    createdAtEpochMs = now,
                ),
            )
        }
        if (dao.getFolder(DEFAULT_DOCUMENTS_ID) == null) {
            dao.upsertFolder(
                FolderEntity(
                    id = DEFAULT_DOCUMENTS_ID,
                    name = "Documents",
                    kind = FolderKind.DOCUMENTS.name,
                    createdAtEpochMs = now + 1,
                ),
            )
        }
    }

    suspend fun createFolder(name: String): FolderEntity {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Folder name required" }
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            kind = FolderKind.CUSTOM.name,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        dao.upsertFolder(folder)
        return folder
    }

    suspend fun deleteFolder(folderId: String) {
        require(folderId != DEFAULT_PHOTOS_ID && folderId != DEFAULT_DOCUMENTS_ID) {
            "Cannot delete built-in folders"
        }
        val files = dao.getAllFiles().filter { it.folderId == folderId }
        files.forEach { deleteFileRecord(it) }
        dao.deleteCustomFolder(folderId)
    }

    suspend fun importFile(folderId: String, uri: Uri): Result<FileItemEntity> = runCatching {
        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val displayName = queryDisplayName(uri) ?: "file-${System.currentTimeMillis()}"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read file")
        val isImage = mimeType.startsWith("image/")
        val storageFile = File(appContext.filesDir, "files_content/${UUID.randomUUID()}.bin")
        EncryptedFileStore.writeEncrypted(fileKey, storageFile, bytes)
        val entity = FileItemEntity(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
            storagePath = storageFile.absolutePath,
            createdAtEpochMs = System.currentTimeMillis(),
            isImage = isImage,
        )
        dao.upsertFile(entity)
        if (isImage) {
            indexFile(entity)
        }
        entity
    }

    suspend fun getFile(id: String): FileItemEntity? = dao.getFile(id)

    fun readFileBytes(entity: FileItemEntity): ByteArray =
        EncryptedFileStore.readEncrypted(fileKey, File(entity.storagePath))

    suspend fun deleteFile(id: String) {
        val entity = dao.getFile(id) ?: return
        deleteFileRecord(entity)
    }

    private suspend fun deleteFileRecord(entity: FileItemEntity) {
        runCatching { File(entity.storagePath).delete() }
        dao.deleteAnalysis(entity.id)
        dao.deleteFileById(entity.id)
    }

    suspend fun indexFile(file: FileItemEntity): Result<Unit> {
        if (!file.isImage) return Result.success(Unit)
        val bytes = readFileBytes(file)
        return LocalVisionEngine.indexImage(appContext, bytes, file.displayName).map { result ->
            dao.upsertAnalysis(
                ImageAnalysisEntity(
                    fileId = file.id,
                    objectLabels = result.objects.joinToString(",") { it.label },
                    ocrText = result.ocrText,
                    faceEmbeddingsJson = EmbeddingCodec.encode(result.faces),
                    searchBlob = result.searchBlob,
                    indexedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun indexAllImages(): Result<Int> = runCatching {
        var count = 0
        dao.getAllFiles().filter { it.isImage }.forEach { file ->
            indexFile(file).onSuccess { count++ }
        }
        count
    }

    suspend fun searchImages(query: String): List<FileItemEntity> {
        val terms = VisionSearch.expandQuery(query)
        if (terms.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        val results = mutableListOf<FileItemEntity>()
        terms.forEach { term ->
            dao.searchImagesByTerm(term).forEach { file ->
                if (seen.add(file.id)) results += file
            }
        }
        return results.sortedByDescending { it.createdAtEpochMs }
    }

    suspend fun findSimilarFaces(fileId: String): List<FileItemEntity> {
        val analysis = dao.getAnalysis(fileId) ?: return emptyList()
        val source = EmbeddingCodec.decode(analysis.faceEmbeddingsJson)
        if (source.isEmpty()) return emptyList()
        val candidates = dao.getAllAnalysis().map { row ->
            row.fileId to EmbeddingCodec.decode(row.faceEmbeddingsJson)
        }
        return FaceMatcher.findSimilar(source, candidates, excludeFileId = fileId)
            .mapNotNull { match -> dao.getFile(match.fileId) }
    }

    suspend fun getAnalysis(fileId: String): ImageAnalysisEntity? = dao.getAnalysis(fileId)

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

    fun verifyBackupPassphrase(passphrase: CharArray): Boolean {
        val stored = syncPrefs.getString(KEY_BACKUP_PASSPHRASE_CHECK) ?: return false
        return stored == hashPassphrase(passphrase)
    }

    fun saveBackupPassphraseCheck(passphrase: CharArray) {
        syncPrefs.putString(KEY_BACKUP_PASSPHRASE_CHECK, hashPassphrase(passphrase))
    }

    private fun syncEngine(): FreedomSyncEngine =
        FreedomSyncEngine(appContext, loadSyncConfig(), namespace = "files", backupFileName = "files.bin")

    private suspend fun exportSnapshot(): String {
        val folders = dao.getAllFolders()
        val files = dao.getAllFiles()
        val root = JSONObject().put("version", 1)
        val folderArray = JSONArray()
        folders.forEach { folder ->
            folderArray.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
                    .put("kind", folder.kind)
                    .put("createdAtEpochMs", folder.createdAtEpochMs),
            )
        }
        val fileArray = JSONArray()
        files.forEach { file ->
            val encryptedBytes = File(file.storagePath).readBytes()
            fileArray.put(
                JSONObject()
                    .put("id", file.id)
                    .put("folderId", file.folderId)
                    .put("displayName", file.displayName)
                    .put("mimeType", file.mimeType)
                    .put("sizeBytes", file.sizeBytes)
                    .put("createdAtEpochMs", file.createdAtEpochMs)
                    .put("isImage", file.isImage)
                    .put("encryptedDataBase64", Base64.getEncoder().encodeToString(encryptedBytes)),
            )
        }
        val analysisArray = JSONArray()
        dao.getAllAnalysis().forEach { analysis ->
            analysisArray.put(
                JSONObject()
                    .put("fileId", analysis.fileId)
                    .put("objectLabels", analysis.objectLabels)
                    .put("ocrText", analysis.ocrText)
                    .put("faceEmbeddingsJson", analysis.faceEmbeddingsJson)
                    .put("searchBlob", analysis.searchBlob)
                    .put("indexedAtEpochMs", analysis.indexedAtEpochMs),
            )
        }
        root.put("folders", folderArray)
        root.put("files", fileArray)
        root.put("image_analysis", analysisArray)
        return root.toString()
    }

    private suspend fun importSnapshot(json: String): Result<Int> {
        return runCatching {
            val root = JSONObject(json)
            val folders = root.getJSONArray("folders")
            val files = root.getJSONArray("files")
            for (i in 0 until folders.length()) {
                val item = folders.getJSONObject(i)
                dao.upsertFolder(
                    FolderEntity(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        kind = item.getString("kind"),
                        createdAtEpochMs = item.getLong("createdAtEpochMs"),
                    ),
                )
            }
            var imported = 0
            for (i in 0 until files.length()) {
                val item = files.getJSONObject(i)
                val storageFile = File(appContext.filesDir, "files_content/${UUID.randomUUID()}.bin")
                val encrypted = Base64.getDecoder().decode(item.getString("encryptedDataBase64"))
                EncryptedFileStore.writeCiphertext(storageFile, encrypted)
                dao.upsertFile(
                    FileItemEntity(
                        id = item.getString("id"),
                        folderId = item.getString("folderId"),
                        displayName = item.getString("displayName"),
                        mimeType = item.getString("mimeType"),
                        sizeBytes = item.getLong("sizeBytes"),
                        storagePath = storageFile.absolutePath,
                        createdAtEpochMs = item.getLong("createdAtEpochMs"),
                        isImage = item.getBoolean("isImage"),
                    ),
                )
                imported++
            }
            if (root.has("image_analysis")) {
                val analysis = root.getJSONArray("image_analysis")
                for (i in 0 until analysis.length()) {
                    val item = analysis.getJSONObject(i)
                    dao.upsertAnalysis(
                        ImageAnalysisEntity(
                            fileId = item.getString("fileId"),
                            objectLabels = item.optString("objectLabels", ""),
                            ocrText = item.optString("ocrText", ""),
                            faceEmbeddingsJson = item.optString("faceEmbeddingsJson", ""),
                            searchBlob = item.optString("searchBlob", ""),
                            indexedAtEpochMs = item.optLong("indexedAtEpochMs", System.currentTimeMillis()),
                        ),
                    )
                }
            } else {
                indexAllImages()
            }
            imported
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = appContext.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return if (extension.isNullOrBlank()) null else "file.$extension"
    }

    private fun hashPassphrase(passphrase: CharArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(String(passphrase).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_PHOTOS_ID = "folder-photos"
        const val DEFAULT_DOCUMENTS_ID = "folder-documents"

        private const val KEY_BACKEND = "backend"
        private const val KEY_WEBDAV_URL = "webdav_url"
        private const val KEY_WEBDAV_EMAIL = "webdav_email"
        private const val KEY_WEBDAV_PASSWORD = "webdav_password"
        private const val KEY_S3_ENDPOINT = "s3_endpoint"
        private const val KEY_S3_BUCKET = "s3_bucket"
        private const val KEY_S3_ACCESS_KEY = "s3_access_key"
        private const val KEY_S3_SECRET_KEY = "s3_secret_key"
        private const val KEY_BACKUP_PASSPHRASE_CHECK = "backup_passphrase_check"
    }
}
