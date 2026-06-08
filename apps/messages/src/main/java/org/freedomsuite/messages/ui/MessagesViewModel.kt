package org.freedomsuite.messages.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig
import org.freedomsuite.messages.data.ChannelEntity
import org.freedomsuite.messages.data.ChannelType
import org.freedomsuite.messages.data.ChannelWithPreview
import org.freedomsuite.messages.data.MessageEntity
import org.freedomsuite.messages.data.MessagesRepository

class MessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MessagesRepository(application)
    private var messagesJob: Job? = null

    val channels: StateFlow<List<ChannelWithPreview>> = repository.observeChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChannel = MutableStateFlow<ChannelEntity?>(null)
    val activeChannel: StateFlow<ChannelEntity?> = _activeChannel.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultPersonalChannel()
        }
    }

    fun openChannel(channelId: String) {
        messagesJob?.cancel()
        viewModelScope.launch {
            _activeChannel.value = repository.getChannel(channelId)
        }
        messagesJob = viewModelScope.launch {
            repository.observeMessages(channelId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun closeChannel() {
        messagesJob?.cancel()
        _activeChannel.value = null
        _messages.value = emptyList()
    }

    fun postMessage(body: String) {
        val channelId = _activeChannel.value?.id ?: return
        viewModelScope.launch {
            repository.postMessage(channelId, body)
        }
    }

    fun postPhoto(uri: Uri, mimeType: String) {
        val channelId = _activeChannel.value?.id ?: return
        viewModelScope.launch {
            repository.postPhoto(channelId, uri, mimeType)
        }
    }

    fun readAttachment(path: String): ByteArray = repository.readAttachment(path)

    fun createChannel(name: String, type: ChannelType, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val channel = repository.createChannel(name, type)
            onCreated(channel.id)
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncNow()
                .onSuccess { _syncStatus.value = it }
                .onFailure { _syncStatus.value = it.message ?: "Sync failed" }
            _isSyncing.value = false
        }
    }

    fun restoreFromSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.restoreFromSync()
                .onSuccess { _syncStatus.value = "Restored $it messages" }
                .onFailure { _syncStatus.value = it.message ?: "Restore failed" }
            _isSyncing.value = false
        }
    }

    fun lastSyncEpochMs(): Long = repository.lastSyncEpochMs()

    fun loadSyncConfig(): SyncConfig = repository.loadSyncConfig()

    fun saveSyncConfig(config: SyncConfig) {
        repository.saveSyncConfig(config)
    }

    fun saveSyncBackend(backend: SyncBackend) {
        repository.saveSyncConfig(repository.loadSyncConfig().copy(backend = backend))
    }
}
