package com.kole.logstel.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_entries")
data class WorkEntryEntity(
    @PrimaryKey val date: String,
    val mainWorker: String,
    val constructionSite: String,
    val otherWorkers: String,
    val hoursWorked: Double,
    val notes: String,
    val attachments: String,
    val updatedAt: Long
)
