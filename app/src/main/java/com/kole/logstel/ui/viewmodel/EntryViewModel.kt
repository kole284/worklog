package com.kole.logstel.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kole.logstel.domain.model.Attachment
import com.kole.logstel.domain.model.AttachmentType
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.domain.repository.WorkEntryRepository
import com.kole.logstel.ui.model.EntryFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkEntryRepository
) : ViewModel() {
    private val date: String = checkNotNull(savedStateHandle["date"])
    private val _state = MutableStateFlow(EntryFormState(date = date))
    val state: StateFlow<EntryFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getByDate(date)?.let { entry ->
                _state.value = EntryFormState(
                    date = entry.date,
                    mainWorker = entry.mainWorker,
                    constructionSite = entry.constructionSite,
                    otherWorkers = entry.otherWorkers,
                    hoursWorked = entry.hoursWorked.toString().trimEnd('0').trimEnd('.'),
                    notes = entry.notes,
                    attachments = entry.attachments
                )
            }
        }
    }

    fun updateMainWorker(value: String) = _state.update { it.copy(mainWorker = value, mainWorkerError = null) }
    fun updateConstructionSite(value: String) = _state.update { it.copy(constructionSite = value, constructionSiteError = null) }
    fun updateHours(value: String) = _state.update {
        val cleaned = buildString {
            var hasDot = false
            value.forEach { char ->
                when {
                    char.isDigit() -> append(char)
                    char == '.' && !hasDot -> {
                        append(char)
                        hasDot = true
                    }
                }
            }
        }
        it.copy(hoursWorked = cleaned, hoursError = null)
    }
    fun updateNotes(value: String) = _state.update { it.copy(notes = value) }
    fun updateNewWorker(value: String) = _state.update { it.copy(newWorker = value) }

    fun addWorker() = _state.update {
        val worker = it.newWorker.trim()
        if (worker.isBlank()) it else it.copy(otherWorkers = it.otherWorkers + worker, newWorker = "")
    }

    fun removeWorker(worker: String) = _state.update { it.copy(otherWorkers = it.otherWorkers - worker) }

    fun addAttachments(items: List<Pair<String, AttachmentType>>) = _state.update {
        val existing = it.attachments.map { attachment -> attachment.uri }.toSet()
        it.copy(attachments = it.attachments + items.filterNot { item -> item.first in existing }.map { item -> Attachment(item.first, item.second) })
    }

    fun removeAttachment(attachment: Attachment) = _state.update { it.copy(attachments = it.attachments - attachment) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun save() {
        val current = state.value
        val selectedDate = runCatching { LocalDate.parse(current.date) }.getOrNull()
        if (selectedDate != null && selectedDate.isAfter(LocalDate.now())) {
            _state.update { it.copy(message = "Future dates cannot be edited.") }
            return
        }
        val mainWorker = current.mainWorker.trim()
        val constructionSite = current.constructionSite.trim()
        val hoursText = current.hoursWorked.trim()
        val hours = hoursText.toDoubleOrNull()
        var next = current.copy(
            mainWorkerError = null,
            constructionSiteError = null,
            hoursError = null
        )

        if (mainWorker.isBlank()) {
            next = next.copy(mainWorkerError = "Main worker is required.")
        }
        if (constructionSite.isBlank()) {
            next = next.copy(constructionSiteError = "Construction site is required.")
        }

        val hoursError = when {
            hoursText.isBlank() -> "Hours worked is required."
            hours == null -> "Enter a valid number of hours."
            hours <= 0.0 -> "Hours must be greater than 0."
            hours > 24.0 -> "Hours cannot exceed 24."
            else -> null
        }
        if (hoursError != null) {
            next = next.copy(hoursError = hoursError)
        }

        if (next.mainWorkerError != null || next.constructionSiteError != null || next.hoursError != null || hours == null) {
            _state.value = next
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.save(
                    WorkEntry(
                        date = current.date,
                        mainWorker = mainWorker,
                        constructionSite = constructionSite,
                        otherWorkers = current.otherWorkers.map { it.trim() }.filter { it.isNotBlank() },
                        hoursWorked = hours,
                        notes = current.notes,
                        attachments = current.attachments
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saved = true, message = "Entry saved.") }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "Unable to save entry.") }
            }
        }
    }
}
