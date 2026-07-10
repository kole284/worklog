package com.kole.logstel.ui.model

import com.kole.logstel.domain.model.Attachment

data class EntryFormState(
    val date: String = "",
    val mainWorker: String = "",
    val constructionSite: String = "",
    val otherWorkers: List<String> = emptyList(),
    val newWorker: String = "",
    val hoursWorked: String = "",
    val notes: String = "",
    val attachments: List<Attachment> = emptyList(),
    val mainWorkerError: String? = null,
    val constructionSiteError: String? = null,
    val hoursError: String? = null,
    val saved: Boolean = false,
    val message: String? = null
)
