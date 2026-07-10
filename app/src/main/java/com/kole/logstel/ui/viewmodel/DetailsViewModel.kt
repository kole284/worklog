package com.kole.logstel.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.domain.repository.WorkEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: WorkEntryRepository
) : ViewModel() {
    private val date: String = checkNotNull(savedStateHandle["date"])
    val entry: StateFlow<WorkEntry?> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
