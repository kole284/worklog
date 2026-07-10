package com.kole.logstel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.domain.repository.WorkEntryRepository
import com.kole.logstel.domain.service.BackupService
import com.kole.logstel.domain.service.PdfExportService
import com.kole.logstel.ui.util.totalWorkedHoursForVisibleMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val entries: List<WorkEntry> = emptyList(),
    val totalHours: BigDecimal = BigDecimal.ZERO,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkEntryRepository,
    private val backupService: BackupService,
    private val pdfExportService: PdfExportService
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val importRefresh = MutableStateFlow(0)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(selectedMonth, importRefresh) { month, _ -> month }.flatMapLatest { month ->
        combine(repository.observeMonth(month), message) { entries, currentMessage ->
            val total = totalWorkedHoursForVisibleMonth(
                month = month,
                entries = entries,
                today = LocalDate.now()
            )
            HomeUiState(
                month = month,
                entries = entries,
                totalHours = total,
                message = currentMessage
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }

    fun clearMessage() = message.update { null }

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching { backupService.writeBackup(uri, repository.getAll()) }
                .onSuccess { message.value = "Backup exported." }
                .onFailure { message.value = it.message ?: "Backup export failed." }
        }
    }

    fun importBackup(entries: List<WorkEntry>) {
        viewModelScope.launch {
            runCatching { repository.replaceAll(entries) }
                .onSuccess {
                    importRefresh.update { it + 1 }
                    message.value = "Backup imported successfully."
                }
                .onFailure { message.value = it.message ?: "Backup import failed." }
        }
    }

    suspend fun readBackup(uri: android.net.Uri): Result<List<WorkEntry>> =
        runCatching { backupService.readBackup(uri) }

    fun pdfFileName(): String = "Worklog_${selectedMonth.value}.pdf"

    fun exportPdf(uri: android.net.Uri) {
        viewModelScope.launch {
            val month = selectedMonth.value
            runCatching {
                pdfExportService.exportMonth(
                    month = month,
                    entries = repository.getAll().filter { YearMonth.from(java.time.LocalDate.parse(it.date)) == month },
                    uri = uri
                )
            }
                .onSuccess { message.value = "PDF exported successfully." }
                .onFailure { message.value = "PDF export failed." }
        }
    }
}
