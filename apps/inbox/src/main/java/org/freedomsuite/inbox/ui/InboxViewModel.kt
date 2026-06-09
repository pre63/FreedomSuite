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
import org.freedomsuite.core.account.discovery.DevMailServer
import org.freedomsuite.core.account.discovery.DiscoverySource
import org.freedomsuite.core.account.discovery.MailServerSettings
import org.freedomsuite.core.ui.ManualMailSettings
import org.freedomsuite.core.account.MailAccount
import org.freedomsuite.inbox.data.InboxRepository
import org.freedomsuite.inbox.data.MailMessageEntity
import org.freedomsuite.protocol.ical.InviteResponseStatus

data class ComposeDraft(
    val to: String = "",
    val subject: String = "",
    val body: String = "",
)

data class AccountInfo(
    val email: String,
    val aliases: List<String>,
    val ownedDomains: List<String>,
)

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

    private val _suggestManualSetup = MutableStateFlow(false)
    val suggestManualSetup: StateFlow<Boolean> = _suggestManualSetup.asStateFlow()

    private val _activeMessage = MutableStateFlow<MailMessageEntity?>(null)
    val activeMessage: StateFlow<MailMessageEntity?> = _activeMessage.asStateFlow()

    private val _calendarInstalled = MutableStateFlow(repository.isCalendarInstalled())
    val calendarInstalled: StateFlow<Boolean> = _calendarInstalled.asStateFlow()

    private val _accountInfo = MutableStateFlow(repository.getAccount()?.toAccountInfo())
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()

    private val _composeDraft = MutableStateFlow<ComposeDraft?>(null)
    val composeDraft: StateFlow<ComposeDraft?> = _composeDraft.asStateFlow()

    init {
        if (_hasAccount.value) {
            loadFolders()
            refresh()
        }
    }

    fun configureAccount(email: String, password: String, manual: ManualMailSettings? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _suggestManualSetup.value = false
            val manualSettings = manual?.toMailServerSettings()
            repository.configureAccount(email, password, manualSettings)
                .onSuccess {
                    _hasAccount.value = true
                    _accountInfo.value = repository.getAccount()?.toAccountInfo()
                    loadFolders()
                    refresh()
                }
                .onFailure { failure ->
                    _error.value = failure.message ?: "Connection failed"
                    _suggestManualSetup.value = manual == null
                }
            _isLoading.value = false
        }
    }

    private fun ManualMailSettings.toMailServerSettings(): MailServerSettings = MailServerSettings(
        imapHost = imapHost,
        imapPort = imapPort,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        plainText = imapPort == DevMailServer.IMAP_PORT && smtpPort == DevMailServer.SMTP_PORT,
        source = DiscoverySource.MANUAL,
        label = "manual",
    )

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

    fun openMessage(uid: Long, folder: String? = null) {
        val targetFolder = folder ?: _currentFolder.value
        if (folder != null && folder != _currentFolder.value) {
            _currentFolder.value = folder
        }
        viewModelScope.launch {
            _activeMessage.value = repository.getMessage(targetFolder, uid)
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

    fun reportSpam(folder: String, uid: Long) {
        viewModelScope.launch {
            repository.reportSpam(folder, uid)
                .onFailure { _error.value = it.message ?: "Could not move to spam" }
        }
    }

    fun markNotSpam(folder: String, uid: Long) {
        viewModelScope.launch {
            repository.markNotSpam(folder, uid)
                .onFailure { _error.value = it.message ?: "Could not restore message" }
            refresh()
        }
    }

    fun isSpamFolder(folder: String): Boolean = repository.isSpamFolder(folder)

    fun startReply(message: MailMessageEntity) {
        val to = extractReplyAddress(message.from)
        val subject = if (message.subject.startsWith("Re:", ignoreCase = true)) {
            message.subject
        } else {
            "Re: ${message.subject}"
        }
        val quoted = message.body.ifBlank { message.snippet }
        _composeDraft.value = ComposeDraft(
            to = to,
            subject = subject,
            body = "\n\n---\n${message.from} wrote:\n$quoted",
        )
    }

    fun clearComposeDraft() {
        _composeDraft.value = null
    }

    fun sendMessage(to: String, subject: String, body: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sendMessage(to, subject, body)
                .onSuccess {
                    _composeDraft.value = null
                    onSent()
                }
                .onFailure { _error.value = it.message ?: "Send failed" }
            _isLoading.value = false
        }
    }

    fun saveMailboxExtras(aliases: List<String>, ownedDomains: List<String>) {
        repository.saveMailboxExtras(aliases, ownedDomains)
        _accountInfo.value = repository.getAccount()?.toAccountInfo()
    }

    fun signOut() {
        repository.signOut()
        _hasAccount.value = false
        _accountInfo.value = null
        _folders.value = listOf(DEFAULT_FOLDER)
        _currentFolder.value = DEFAULT_FOLDER
        _activeMessage.value = null
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

    private fun MailAccount.toAccountInfo() = AccountInfo(
        email = email,
        aliases = aliases,
        ownedDomains = ownedDomains,
    )

    private fun extractReplyAddress(from: String): String {
        val angle = Regex("""<([^>]+)>""").find(from)?.groupValues?.get(1)
        return angle ?: from.trim()
    }

    companion object {
        const val DEFAULT_FOLDER = "INBOX"
    }
}
