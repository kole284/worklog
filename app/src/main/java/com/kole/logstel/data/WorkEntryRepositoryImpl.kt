package com.kole.logstel.data

import com.kole.logstel.data.local.WorkEntryDao
import com.kole.logstel.domain.model.WorkEntry
import com.kole.logstel.domain.repository.WorkEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class WorkEntryRepositoryImpl @Inject constructor(
    private val dao: WorkEntryDao
) : WorkEntryRepository {
    override fun observeAll(): Flow<List<WorkEntry>> =
        dao.observeAll().map { entries -> entries.map { it.toDomain() } }

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> {
        val start = month.atDay(1).toString()
        val end = month.atEndOfMonth().toString()
        return dao.observeMonth(start, end).map { entries -> entries.map { it.toDomain() } }
    }

    override fun observeByDate(date: String): Flow<WorkEntry?> =
        dao.observeByDate(date).map { it?.toDomain() }

    override suspend fun getByDate(date: String): WorkEntry? = dao.getByDate(date)?.toDomain()

    override suspend fun getAll(): List<WorkEntry> = dao.getAll().map { it.toDomain() }

    override suspend fun save(entry: WorkEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun replaceAll(entries: List<WorkEntry>) {
        dao.replaceAll(entries.map { it.toEntity() })
    }
}
