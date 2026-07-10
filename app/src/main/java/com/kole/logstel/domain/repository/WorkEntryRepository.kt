package com.kole.logstel.domain.repository

import com.kole.logstel.domain.model.WorkEntry
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface WorkEntryRepository {
    fun observeAll(): Flow<List<WorkEntry>>
    fun observeMonth(month: YearMonth): Flow<List<WorkEntry>>
    fun observeByDate(date: String): Flow<WorkEntry?>
    suspend fun getByDate(date: String): WorkEntry?
    suspend fun getAll(): List<WorkEntry>
    suspend fun save(entry: WorkEntry)
    suspend fun replaceAll(entries: List<WorkEntry>)
}
