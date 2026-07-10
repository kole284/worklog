package com.kole.logstel.ui.util

import com.kole.logstel.domain.model.WorkEntry
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal fun totalWorkedHoursForVisibleMonth(
    month: YearMonth,
    entries: List<WorkEntry>,
    today: LocalDate = LocalDate.now()
): BigDecimal {
    val (startDate, endDate) = visibleMonthDateRange(month, today) ?: return BigDecimal.ZERO

    return entries
        .asSequence()
        .filter { entry ->
            val entryDate = runCatching { LocalDate.parse(entry.date) }.getOrNull()
            entryDate != null && entryDate >= startDate && entryDate <= endDate
        }
        .fold(BigDecimal.ZERO) { total, entry ->
            total + BigDecimal.valueOf(entry.hoursWorked)
        }
}

internal fun formatHours(hours: BigDecimal): String =
    "${hours.stripTrailingZeros().toPlainString()} h"

internal fun visibleMonthDateRange(
    month: YearMonth,
    today: LocalDate = LocalDate.now()
): Pair<LocalDate, LocalDate>? {
    val startDate = month.atDay(1)
    if (startDate.isAfter(today)) return null

    val endDate = if (month == YearMonth.from(today)) {
        today
    } else {
        month.atEndOfMonth()
    }
    return startDate to endDate
}
