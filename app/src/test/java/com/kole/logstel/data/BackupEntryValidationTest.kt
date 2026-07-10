package com.kole.logstel.data

import org.junit.Assert.assertThrows
import org.junit.Test

class BackupEntryValidationTest {
    @Test
    fun acceptsValidBackupEntryFields() {
        validateBackupEntryFields(
            date = "2026-07-10",
            mainWorker = "Worker",
            constructionSite = "Site",
            hoursWorked = 7.5
        )
    }

    @Test
    fun rejectsInvalidCalendarDate() {
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupEntryFields(
                date = "2026-02-30",
                mainWorker = "Worker",
                constructionSite = "Site",
                hoursWorked = 7.5
            )
        }
    }

    @Test
    fun rejectsHoursGreaterThanTwentyFour() {
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupEntryFields(
                date = "2026-07-10",
                mainWorker = "Worker",
                constructionSite = "Site",
                hoursWorked = 25.0
            )
        }
    }

    @Test
    fun rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupEntryFields(
                date = "2026-07-10",
                mainWorker = " ",
                constructionSite = "Site",
                hoursWorked = 7.5
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupEntryFields(
                date = "2026-07-10",
                mainWorker = "Worker",
                constructionSite = " ",
                hoursWorked = 7.5
            )
        }
    }
}
