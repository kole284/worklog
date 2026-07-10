package com.kole.logstel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.domain.repository.WorkEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<WorkEntry> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: WorkEntryRepository
) : ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = combine(query, repository.observeAll()) { value, entries ->
        val normalized = value.trim().lowercase()
        val results = if (normalized.isBlank()) entries else entries.filter { entry ->
            entry.constructionSite.lowercase().contains(normalized) ||
                entry.date.contains(normalized) ||
                entry.mainWorker.lowercase().contains(normalized) ||
                entry.otherWorkers.any { it.lowercase().contains(normalized) }
        }
        SearchUiState(value, results)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun updateQuery(value: String) = query.update { value }
}
