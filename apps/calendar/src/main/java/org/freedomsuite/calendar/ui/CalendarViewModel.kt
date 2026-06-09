package org.freedomsuite.calendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.freedomsuite.calendar.data.CalendarRepository
import org.freedomsuite.calendar.data.EventEntity

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(application)

    val events: StateFlow<List<EventEntity>> = repository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeEvent = MutableStateFlow<EventEntity?>(null)
    val activeEvent: StateFlow<EventEntity?> = _activeEvent.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultCalendar()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.syncBackup()
                .onFailure { _error.value = it.message ?: "Backup failed" }
            _isLoading.value = false
        }
    }

    fun openEvent(uid: String) {
        viewModelScope.launch {
            _activeEvent.value = repository.getEvent(uid)
        }
    }

    fun closeEvent() {
        _activeEvent.value = null
    }

    fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        isAllDay: Boolean = false,
        reminderMinutesBefore: Int? = null,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createEvent(
                title = title,
                description = description,
                location = location,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                isAllDay = isAllDay,
                reminderMinutesBefore = reminderMinutesBefore,
            )
                .onSuccess { onCreated(it.uid) }
                .onFailure { _error.value = it.message ?: "Create failed" }
            _isLoading.value = false
        }
    }

    fun updateEvent(
        uid: String,
        title: String,
        description: String?,
        location: String?,
        startEpochMs: Long,
        endEpochMs: Long,
        isAllDay: Boolean = false,
        reminderMinutesBefore: Int? = null,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateEvent(
                uid = uid,
                title = title,
                description = description,
                location = location,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                isAllDay = isAllDay,
                reminderMinutesBefore = reminderMinutesBefore,
            )
                .onSuccess {
                    _activeEvent.value = it
                    onSaved()
                }
                .onFailure { _error.value = it.message ?: "Update failed" }
            _isLoading.value = false
        }
    }

    fun deleteEvent(uid: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEvent(uid)
                .onSuccess { onDeleted() }
                .onFailure { _error.value = it.message ?: "Delete failed" }
        }
    }
}
