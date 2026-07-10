package com.kole.logstel.ui.util

import com.kole.logstel.domain.model.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class MonthlyHoursTest {
    private val today = LocalDate.of(2026, 7, 9)

    @Test
    fun currentMonthSumsMultipleEntriesBeforeToday() {
        val total = totalWorkedHoursForVisibleMonth(
            month = YearMonth.of(2026, 7),
            entries = listOf(
                entry("2026-07-01", 8.0),
                entry("2026-07-02", 7.5),
                entry("2026-07-03", 6.0)
            ),
            today = today
        )

        assertEquals(BigDecimal("21.5"), total)
    }

    @Test
    fun currentMonthSumsAllMatchingDailyHoursInsteadOfTakingMaximum() {
        val total = totalWorkedHoursForVisibleMonth(
            month = YearMonth.of(2026, 7),
            entries = listOf(
                entry("2026-07-01", 6.0),
                entry("2026-07-02", 5.0),
                entry("2026-07-03", 5.0),
                entry("2026-07-04", 4.0)
            ),
            today = today
        )

        assertEquals(BigDecimal("20.0"), total)
        assertEquals("20 h", formatHours(total))
    }

    @Test
    fun currentMonthDoesNotIncludeEntriesAfterToday() {
        val total = totalWorkedHoursForVisibleMonth(
            month = YearMonth.of(2026, 7),
            entries = listOf(
                entry("2026-07-01", 8.0),
                entry("2026-07-10", 7.5)
            ),
            today = today
        )

        assertEquals(BigDecimal("8.0"), total)
    }

    @Test
    fun pastMonthIncludesWholeMonth() {
        val total = totalWorkedHoursForVisibleMonth(
            month = YearMonth.of(2026, 6),
            entries = listOf(
                entry("2026-06-01", 8.0),
                entry("2026-06-30", 7.5),
                entry("2026-07-01", 6.0)
            ),
            today = today
        )

        assertEquals(BigDecimal("15.5"), total)
    }

    @Test
    fun futureMonthShowsZero() {
        val total = totalWorkedHoursForVisibleMonth(
            month = YearMonth.of(2026, 8),
            entries = listOf(entry("2026-08-01", 8.0)),
            today = today
        )

        assertEquals(BigDecimal.ZERO, total)
    }

    @Test
    fun visibleMonthDateRangeUsesTodayForCurrentMonth() {
        assertEquals(
            LocalDate.of(2026, 7, 1) to LocalDate.of(2026, 7, 9),
            visibleMonthDateRange(YearMonth.of(2026, 7), today)
        )
    }

    @Test
    fun visibleMonthDateRangeUsesEndOfMonthForPastMonth() {
        assertEquals(
            LocalDate.of(2026, 6, 1) to LocalDate.of(2026, 6, 30),
            visibleMonthDateRange(YearMonth.of(2026, 6), today)
        )
    }

    @Test
    fun visibleMonthDateRangeReturnsNullForFutureMonth() {
        assertEquals(null, visibleMonthDateRange(YearMonth.of(2026, 8), today))
    }

    @Test
    fun formatsHoursWithoutUnnecessaryTrailingZeros() {
        assertEquals("20 h", formatHours(BigDecimal("20.0")))
        assertEquals("20.5 h", formatHours(BigDecimal("20.5")))
        assertEquals("20.25 h", formatHours(BigDecimal("20.25")))
        assertEquals("8 h", formatHours(BigDecimal("8.0")))
        assertEquals("7.5 h", formatHours(BigDecimal("7.5")))
        assertEquals("10.25 h", formatHours(BigDecimal("10.25")))
        assertEquals("0 h", formatHours(BigDecimal("0.0")))
    }

    private fun entry(date: String, hoursWorked: Double) = WorkEntry(
        date = date,
        mainWorker = "Worker",
        constructionSite = "Site",
        otherWorkers = emptyList(),
        hoursWorked = hoursWorked,
        notes = "",
        attachments = emptyList()
    )
}
