package com.kole.logstel.domain.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.kole.logstel.domain.model.WorkEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.max

class PdfExportService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val displayDate = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun exportMonth(month: YearMonth, entries: List<WorkEntry>, uri: Uri) {
        val document = PdfDocument()
        try {
            val pageWidth = 842
            val pageHeight = 595
            val margin = 32f
            val rowHeight = 34f
            val bottom = pageHeight - 42f
            var pageNumber = 0
            var y = 0f
            lateinit var canvas: Canvas
            var page: PdfDocument.Page? = null

            fun startPage() {
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page!!.canvas
                y = margin
                drawTitle(canvas, month, margin)
                y += 42f
                drawHeader(canvas, y, margin, pageWidth - margin)
                y += rowHeight
            }

            fun finishPage() {
                drawFooter(canvas, pageNumber, pageWidth, pageHeight)
                document.finishPage(page)
            }

            startPage()
            val workedEntries = entries.sortedBy { it.date }
            workedEntries.forEach { entry ->
                val notesLines = wrap(entry.notes.replace('\n', ' '), 120)
                val workerLines = wrap(entry.otherWorkers.joinToString(", "), 48)
                val siteLines = wrap(entry.constructionSite, 34)
                val lines = max(1, max(notesLines.size, max(workerLines.size, siteLines.size)))
                val height = max(rowHeight, 18f + lines * 13f)
                if (y + height > bottom) {
                    finishPage()
                    startPage()
                }
                drawRow(canvas, entry, y, height, margin, pageWidth - margin, notesLines, workerLines, siteLines)
                y += height
            }
            val total = workedEntries.sumOf { it.hoursWorked }
            if (y + 44f > bottom) {
                finishPage()
                startPage()
            }
            drawTotal(canvas, total, y + 22f, margin, pageWidth - margin)
            finishPage()
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                document.writeTo(output)
            } ?: error("Unable to open selected PDF file.")
        } finally {
            document.close()
        }
    }

    private fun drawTitle(canvas: Canvas, month: YearMonth, left: Float) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 87, 56)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        canvas.drawText("Worklog", left, 34f, titlePaint)
        canvas.drawText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), left, 52f, subtitlePaint)
    }

    private fun drawHeader(canvas: Canvas, top: Float, left: Float, right: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(21, 87, 56) }
        canvas.drawRect(left, top, right, top + 28f, paint)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        listOf("Date", "Main Worker", "Construction Site", "Other Workers", "Hours", "Notes").forEachIndexed { index, label ->
            canvas.drawText(label, columns(left)[index] + 5f, top + 18f, text)
        }
    }

    private fun drawRow(
        canvas: Canvas,
        entry: WorkEntry,
        top: Float,
        height: Float,
        left: Float,
        right: Float,
        notesLines: List<String>,
        workerLines: List<String>,
        siteLines: List<String>
    ) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 225, 220); strokeWidth = 1f }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 30, 30); textSize = 9f }
        canvas.drawLine(left, top + height, right, top + height, line)
        val cols = columns(left)
        canvas.drawText(java.time.LocalDate.parse(entry.date).format(displayDate), cols[0] + 5f, top + 17f, text)
        canvas.drawText(entry.mainWorker, cols[1] + 5f, top + 17f, text)
        drawLines(canvas, siteLines, cols[2] + 5f, top + 17f, text)
        drawLines(canvas, workerLines, cols[3] + 5f, top + 17f, text)
        canvas.drawText("${entry.hoursWorked} h", cols[4] + 5f, top + 17f, text)
        drawLines(canvas, notesLines, cols[5] + 5f, top + 17f, text)
    }

    private fun drawLines(canvas: Canvas, lines: List<String>, x: Float, y: Float, paint: Paint) {
        lines.take(8).forEachIndexed { index, line -> canvas.drawText(line, x, y + index * 13f, paint) }
    }

    private fun drawTotal(canvas: Canvas, total: Double, y: Float, left: Float, right: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 87, 56)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Total Hours: ${formatHours(total)} h", right - 190f, y, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, pageWidth: Int, pageHeight: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 9f }
        canvas.drawText("Page $pageNumber", pageWidth - 74f, pageHeight - 20f, paint)
    }

    private fun columns(left: Float): List<Float> = listOf(left, left + 86f, left + 206f, left + 356f, left + 506f, left + 566f)

    private fun wrap(value: String, maxChars: Int): List<String> {
        if (value.isBlank()) return listOf("")
        val words = value.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            current = if (current.isBlank()) word else if (current.length + word.length + 1 <= maxChars) "$current $word" else {
                lines += current
                word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun formatHours(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
}
