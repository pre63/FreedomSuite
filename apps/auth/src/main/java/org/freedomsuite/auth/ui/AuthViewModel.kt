package org.freedomsuite.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.freedomsuite.auth.data.AuthCode
import org.freedomsuite.auth.data.AuthRepository
import org.freedomsuite.core.totp.TotpAlgorithm
import org.freedomsuite.sync.SyncBackend
import org.freedomsuite.sync.SyncConfig

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tick = MutableStateFlow(System.currentTimeMillis())

    val codes: StateFlow<List<AuthCode>> = combine(accounts, tick) { list, now ->
        repository.currentCodes(list, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick.value = System.currentTimeMillis()
            }
        }
    }

    fun addFromUri(uri: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.addFromOtpAuth(uri)
                .onSuccess { onAdded() }
                .onFailure { _error.value = it.message ?: "Invalid otpauth URI" }
            _isLoading.value = false
        }
    }

    fun addManual(
        issuer: String,
        accountName: String,
        secret: String,
        onAdded: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.addManual(issuer, accountName, secret)
                .onSuccess { onAdded() }
                .onFailure { _error.value = it.message ?: "Could not add account" }
            _isLoading.value = false
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }

    fun backupNow(passphrase: CharArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.backupNow(passphrase)
                .onSuccess {
                    repository.setBackupEnabled(true)
                    repository.saveBackupPassphraseHint(passphrase)
                    _status.value = it
                }
                .onFailure { _error.value = it.message ?: "Backup failed" }
            _isLoading.value = false
        }
    }

    fun restoreFromBackup(passphrase: CharArray) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.restoreFromBackup(passphrase)
                .onSuccess { _status.value = "Restored $it accounts" }
                .onFailure { _error.value = it.message ?: "Restore failed" }
            _isLoading.value = false
        }
    }

    fun isBackupEnabled(): Boolean = repository.isBackupEnabled()

    fun hasBackupPassphraseConfigured(): Boolean = repository.hasBackupPassphraseConfigured()

    fun verifyBackupPassphrase(passphrase: CharArray): Boolean =
        repository.verifyBackupPassphrase(passphrase)

    fun saveBackupPassphrase(passphrase: CharArray) {
        repository.saveBackupPassphraseHint(passphrase)
    }

    fun lastBackupEpochMs(): Long = repository.lastBackupEpochMs()

    fun loadSyncConfig(): SyncConfig = repository.loadSyncConfig()

    fun saveSyncConfig(config: SyncConfig) {
        repository.saveSyncConfig(config)
    }

    fun saveSyncBackend(backend: SyncBackend) {
        repository.saveSyncConfig(repository.loadSyncConfig().copy(backend = backend))
    }

    fun clearMessages() {
        _error.value = null
        _status.value = null
    }
}
