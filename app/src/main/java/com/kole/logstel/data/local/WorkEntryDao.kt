package com.kole.logstel.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {
    @Query("SELECT * FROM work_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun observeMonth(startDate: String, endDate: String): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<WorkEntryEntity?>

    @Query("SELECT * FROM work_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WorkEntryEntity?

    @Query("SELECT * FROM work_entries ORDER BY date ASC")
    suspend fun getAll(): List<WorkEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WorkEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<WorkEntryEntity>)

    @Query("DELETE FROM work_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entries: List<WorkEntryEntity>) {
        deleteAll()
        upsertAll(entries)
    }
}
