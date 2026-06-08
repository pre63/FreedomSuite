package org.freedomsuite.inbox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.freedomsuite.inbox.data.InboxRepository
import org.freedomsuite.inbox.data.MailMessageEntity
import org.freedomsuite.protocol.ical.InviteResponseStatus

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InboxRepository(application)

    private val _hasAccount = MutableStateFlow(repository.hasAccount())
    val hasAccount: StateFlow<Boolean> = _hasAccount.asStateFlow()

    private val _currentFolder = MutableStateFlow(DEFAULT_FOLDER)
    val currentFolder: StateFlow<String> = _currentFolder.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(listOf(DEFAULT_FOLDER))
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val messages: StateFlow<List<MailMessageEntity>> = combine(_currentFolder, _searchQuery) { folder, query ->
        folder to query
    }.flatMapLatest { (folder, query) ->
        if (query.isBlank()) {
            repository.observeFolder(folder)
        } else {
            repository.observeFolderSearch(folder, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeMessage = MutableStateFlow<MailMessageEntity?>(null)
    val activeMessage: StateFlow<MailMessageEntity?> = _activeMessage.asStateFlow()

    private val _calendarInstalled = MutableStateFlow(repository.isCalendarInstalled())
    val calendarInstalled: StateFlow<Boolean> = _calendarInstalled.asStateFlow()

    init {
        if (_hasAccount.value) {
            loadFolders()
            refresh()
        }
    }

    fun configureAccount(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.configureAccount(email, password)
                .onSuccess {
                    _hasAccount.value = true
                    loadFolders()
                    refresh()
                }
                .onFailure { _error.value = it.message ?: "Connection failed" }
            _isLoading.value = false
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            repository.listFolders()
                .onSuccess { listed ->
                    _folders.value = listed.ifEmpty { listOf(DEFAULT_FOLDER) }
                }
        }
    }

    fun selectFolder(folder: String) {
        if (folder == _currentFolder.value) return
        _currentFolder.value = folder
        clearSearch()
        refresh()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.searchFolder(_currentFolder.value, query)
                .onFailure { _error.value = it.message ?: "Search failed" }
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _calendarInstalled.value = repository.isCalendarInstalled()
            repository.syncFolder(_currentFolder.value)
                .onFailure { _error.value = it.message ?: "Sync failed" }
            _isLoading.value = false
        }
    }

    fun openMessage(uid: Long) {
        viewModelScope.launch {
            _activeMessage.value = repository.getMessage(_currentFolder.value, uid)
        }
    }

    fun closeMessage() {
        _activeMessage.value = null
    }

    fun archiveMessage(folder: String, uid: Long) {
        viewModelScope.launch {
            repository.archiveMessage(folder, uid)
                .onFailure { _error.value = it.message ?: "Archive failed" }
        }
    }

    fun sendMessage(to: String, subject: String, body: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sendMessage(to, subject, body)
                .onSuccess { onSent() }
                .onFailure { _error.value = it.message ?: "Send failed" }
            _isLoading.value = false
        }
    }

    fun respondToInvite(response: InviteResponseStatus) {
        val message = _activeMessage.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.respondToInvite(message.folder, message.uid, response)
                .onSuccess {
                    _activeMessage.value = repository.getMessage(message.folder, message.uid)
                }
                .onFailure { _error.value = it.message ?: "Could not respond to invite" }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        const val DEFAULT_FOLDER = "INBOX"
    }
}
