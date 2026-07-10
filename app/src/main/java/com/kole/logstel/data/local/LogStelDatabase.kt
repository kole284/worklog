package com.kole.logstel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LogStelDatabase : RoomDatabase() {
    abstract fun workEntryDao(): WorkEntryDao
}
