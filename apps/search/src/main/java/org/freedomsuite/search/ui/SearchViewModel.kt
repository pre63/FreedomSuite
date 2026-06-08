package org.freedomsuite.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchHit
import org.freedomsuite.core.searchapi.UnifiedSearchClient

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val client = UnifiedSearchClient(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _groupedResults = MutableStateFlow<Map<SearchBridge.Source, List<SearchHit>>>(emptyMap())
    val groupedResults: StateFlow<Map<SearchBridge.Source, List<SearchHit>>> = _groupedResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _installedSources = MutableStateFlow<List<SearchBridge.Source>>(emptyList())
    val installedSources: StateFlow<List<SearchBridge.Source>> = _installedSources.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshInstalledSources()
    }

    fun updateQuery(value: String) {
        _query.value = value
        scheduleSearch()
    }

    fun clearQuery() {
        _query.value = ""
        _groupedResults.value = emptyMap()
        searchJob?.cancel()
        _isSearching.value = false
    }

    fun submitSearch() {
        searchJob?.cancel()
        runSearch(_query.value)
    }

    fun openHit(hit: SearchHit) {
        client.openHit(hit)
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        val trimmed = _query.value.trim()
        if (trimmed.isEmpty()) {
            _groupedResults.value = emptyMap()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runSearch(trimmed)
        }
    }

    private fun runSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val grouped = withContext(Dispatchers.IO) {
                client.grouped(query)
            }
            _groupedResults.value = grouped
            _isSearching.value = false
        }
    }

    private fun refreshInstalledSources() {
        val ctx = getApplication<Application>()
        _installedSources.value = SearchBridge.Source.entries.filter { source ->
            listOf(source.packageName, "${source.packageName}.dev").any { pkg ->
                runCatching {
                    ctx.packageManager.getPackageInfo(pkg, 0)
                    true
                }.getOrDefault(false)
            }
        }
    }
}
