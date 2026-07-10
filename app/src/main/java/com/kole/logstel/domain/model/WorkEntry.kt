package com.kole.logstel.domain.model

data class WorkEntry(
    val date: String,
    val mainWorker: String,
    val constructionSite: String,
    val otherWorkers: List<String>,
    val hoursWorked: Double,
    val notes: String,
    val attachments: List<Attachment>
)
