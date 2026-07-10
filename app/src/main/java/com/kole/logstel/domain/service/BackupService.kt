package com.kole.logstel.domain.service

import android.content.Context
import android.net.Uri
import com.kole.logstel.data.toBackupJson
import com.kole.logstel.data.toEntity
import com.kole.logstel.data.toEntityFromBackup
import com.kole.logstel.data.toDomain
import com.kole.logstel.domain.model.WorkEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class BackupService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun writeBackup(uri: Uri, entries: List<WorkEntry>) {
        val root = JSONObject()
            .put("app", "Worklog")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("entries", JSONArray(entries.map { it.toEntity().toBackupJson() }))

        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(root.toString(2).toByteArray())
        } ?: error("Unable to open selected backup file.")
    }

    fun readBackup(uri: Uri): List<WorkEntry> {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to open selected backup file.")
        val root = JSONObject(content)
        require(root.getString("app") in setOf("Worklog", "BauSite Work Log")) { "This file is not a Worklog backup." }
        require(root.getInt("version") == 1) { "Unsupported backup version." }
        val array = root.getJSONArray("entries")
        val entries = List(array.length()) { index -> array.getJSONObject(index).toEntityFromBackup().toDomain() }
        val duplicate = entries.groupBy { it.date }.values.firstOrNull { it.size > 1 }
        require(duplicate == null) { "Backup contains more than one entry for a date." }
        return entries.sortedBy { it.date }
    }
}
