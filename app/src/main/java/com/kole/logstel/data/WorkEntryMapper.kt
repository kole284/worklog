package com.kole.logstel.data

import com.kole.logstel.data.local.WorkEntryEntity
import com.kole.logstel.domain.model.Attachment
import com.kole.logstel.domain.model.AttachmentType
import com.kole.logstel.domain.model.WorkEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

fun WorkEntryEntity.toDomain(): WorkEntry = WorkEntry(
    date = date,
    mainWorker = mainWorker,
    constructionSite = constructionSite,
    otherWorkers = decodeWorkers(otherWorkers),
    hoursWorked = hoursWorked,
    notes = notes,
    attachments = decodeAttachments(attachments)
)

fun WorkEntry.toEntity(): WorkEntryEntity = WorkEntryEntity(
    date = date,
    mainWorker = mainWorker.trim(),
    constructionSite = constructionSite.trim(),
    otherWorkers = encodeWorkers(otherWorkers),
    hoursWorked = hoursWorked,
    notes = notes,
    attachments = encodeAttachments(attachments),
    updatedAt = System.currentTimeMillis()
)

fun WorkEntryEntity.toBackupJson(): JSONObject = JSONObject()
    .put("date", date)
    .put("mainWorker", mainWorker)
    .put("constructionSite", constructionSite)
    .put("otherWorkers", JSONArray(otherWorkers))
    .put("hoursWorked", hoursWorked)
    .put("notes", notes)
    .put("attachments", JSONArray(attachments))
    .put("updatedAt", updatedAt)

fun JSONObject.toEntityFromBackup(): WorkEntryEntity {
    val date = getString("date")
    val hours = getDouble("hoursWorked")
    val mainWorker = getString("mainWorker").trim()
    val constructionSite = getString("constructionSite").trim()
    validateBackupEntryFields(
        date = date,
        mainWorker = mainWorker,
        constructionSite = constructionSite,
        hoursWorked = hours
    )
    return WorkEntryEntity(
        date = date,
        mainWorker = mainWorker,
        constructionSite = constructionSite,
        otherWorkers = getJSONArray("otherWorkers").toString(),
        hoursWorked = hours,
        notes = optString("notes"),
        attachments = getJSONArray("attachments").toString(),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )
}

internal fun validateBackupEntryFields(
    date: String,
    mainWorker: String,
    constructionSite: String,
    hoursWorked: Double
) {
    require(Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) { "Invalid entry date: $date" }
    require(runCatching { LocalDate.parse(date) }.isSuccess) { "Invalid entry date: $date" }
    require(mainWorker.isNotBlank()) { "Missing main worker for $date" }
    require(constructionSite.isNotBlank()) { "Missing construction site for $date" }
    require(hoursWorked > 0.0 && hoursWorked <= 24.0) { "Invalid hours for $date" }
}

private fun encodeWorkers(workers: List<String>): String {
    val array = JSONArray()
    workers.map { it.trim() }.filter { it.isNotBlank() }.forEach(array::put)
    return array.toString()
}

private fun decodeWorkers(value: String): List<String> = runCatching {
    val array = JSONArray(value)
    List(array.length()) { array.getString(it) }
}.getOrDefault(emptyList())

private fun encodeAttachments(attachments: List<Attachment>): String {
    val array = JSONArray()
    attachments.forEach { attachment ->
        array.put(
            JSONObject()
                .put("uri", attachment.uri)
                .put("type", attachment.type.name)
        )
    }
    return array.toString()
}

private fun decodeAttachments(value: String): List<Attachment> = runCatching {
    val array = JSONArray(value)
    List(array.length()) { index ->
        val item = array.getJSONObject(index)
        Attachment(
            uri = item.getString("uri"),
            type = AttachmentType.valueOf(item.getString("type"))
        )
    }
}.getOrDefault(emptyList())
