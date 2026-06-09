package org.freedomsuite.files.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.freedomsuite.core.ocr.LocalOcrEngine
import org.freedomsuite.files.data.FileItemEntity
import org.freedomsuite.files.data.FilesRepository
import org.freedomsuite.files.data.FolderWithCount
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FilesRepository(application)

    val folders: StateFlow<List<FolderWithCount>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeFolderId = MutableStateFlow<String?>(null)
    val activeFolderId: StateFlow<String?> = _activeFolderId.asStateFlow()

    val folderFiles: StateFlow<List<FileItemEntity>> = _activeFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) flowOf(emptyList()) else repository.observeFiles(folderId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _extractedText = MutableStateFlow<String?>(null)
    val extractedText: StateFlow<String?> = _extractedText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItemEntity>>(emptyList())
    val searchResults: StateFlow<List<FileItemEntity>> = _searchResults.asStateFlow()

    private val _similarFaces = MutableStateFlow<List<FileItemEntity>>(emptyList())
    val similarFaces: StateFlow<List<FileItemEntity>> = _similarFaces.asStateFlow()

    private val _analysisLabels = MutableStateFlow<String?>(null)
    val analysisLabels: StateFlow<String?> = _analysisLabels.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultFolders()
        }
    }

    fun openFolder(folderId: String) {
        _activeFolderId.value = folderId
    }

    fun clearActiveFolder() {
        _activeFolderId.value = null
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { repository.createFolder(name) }
                .onSuccess { _status.value = "Folder created" }
                .onFailure { _error.value = it.message ?: "Could not create folder" }
            _isLoading.value = false
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { repository.deleteFolder(folderId) }
                .onSuccess { _status.value = "Folder deleted" }
                .onFailure { _error.value = it.message ?: "Could not delete folder" }
            _isLoading.value = false
        }
    }

    fun importFile(uri: Uri) {
        val folderId = _activeFolderId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.importFile(folderId, uri)
                .onSuccess { _status.value = "Added ${it.displayName}" }
                .onFailure { _error.value = it.message ?: "Import failed" }
            _isLoading.value = false
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            repository.deleteFile(fileId)
            _status.value = "File deleted"
        }
    }

    suspend fun getFile(id: String): FileItemEntity? = repository.getFile(id)

    fun readFileBytes(entity: FileItemEntity): ByteArray = repository.readFileBytes(entity)

    fun exportFileToUri(entity: FileItemEntity, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                val bytes = repository.readFileBytes(entity)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(bytes)
                } ?: error("Could not open export destination")
            }.onSuccess { _status.value = "Exported ${entity.displayName}" }
                .onFailure { _error.value = it.message ?: "Export failed" }
            _isLoading.value = false
        }
    }

    fun extractTextFromImage(fileId: String) {
        viewModelScope.launch {
            val file = repository.getFile(fileId) ?: return@launch
            if (!file.isImage) return@launch
            _isLoading.value = true
            _error.value = null
            LocalOcrEngine.recognize(getApplication(), repository.readFileBytes(file))
                .onSuccess { result ->
                    _extractedText.value = result.text.takeIf { it.isNotBlank() }
                    if (result.text.isBlank()) {
                        _status.value = "No text found in this image"
                    } else {
                        _status.value = "Text ready — select and copy"
                    }
                }
                .onFailure { _error.value = it.message ?: "Text extraction failed" }
            _isLoading.value = false
        }
    }

    fun clearExtractedText() {
        _extractedText.value = null
    }

    fun copyExtractedText(context: Context) {
        val text = _extractedText.value ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("freedom-files-ocr", text))
        _status.value = "Copied to clipboard"
    }

    fun searchPhotos(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _searchResults.value = repository.searchImages(query)
            if (_searchResults.value.isEmpty() && query.isNotBlank()) {
                _status.value = "No matches — try indexing photos in Settings"
            }
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun loadPhotoAnalysis(fileId: String) {
        viewModelScope.launch {
            _similarFaces.value = repository.findSimilarFaces(fileId)
            val analysis = repository.getAnalysis(fileId)
            _analysisLabels.value = analysis?.objectLabels?.takeIf { it.isNotBlank() }
        }
    }

    fun indexAllPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.indexAllImages()
                .onSuccess { _status.value = "Indexed $it photos locally" }
                .onFailure { _error.value = it.message ?: "Indexing failed" }
            _isLoading.value = false
        }
    }

    fun loadSyncConfig(): SyncConfig = repository.loadSyncConfig()

    fun saveSyncConfig(config: SyncConfig) {
        repository.saveSyncConfig(config)
        _status.value = "Sync settings saved"
    }

    fun saveSyncBackend(backend: SyncBackend) {
        saveSyncConfig(repository.loadSyncConfig().copy(backend = backend))
    }

    fun backupNow(passphrase: CharArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.saveBackupPassphraseCheck(passphrase)
            repository.backupNow(passphrase)
                .onSuccess { _status.value = it }
                .onFailure { _error.value = it.message ?: "Backup failed" }
            _isLoading.value = false
        }
    }

    fun restoreBackup(passphrase: CharArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.restoreFromBackup(passphrase)
                .onSuccess { _status.value = "Restored $it files" }
                .onFailure { _error.value = it.message ?: "Restore failed" }
            _isLoading.value = false
        }
    }

    fun lastBackupEpochMs(): Long = repository.lastBackupEpochMs()

    fun clearMessages() {
        _status.value = null
        _error.value = null
    }
}
