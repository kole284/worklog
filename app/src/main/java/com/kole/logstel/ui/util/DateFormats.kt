package com.kole.logstel.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val DayLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
val MonthLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

fun String.toDisplayDate(): String = LocalDate.parse(this).format(DayLabelFormatter)

fun YearMonth.monthBounds(): Pair<String, String> = atDay(1).toString() to atEndOfMonth().toString()
