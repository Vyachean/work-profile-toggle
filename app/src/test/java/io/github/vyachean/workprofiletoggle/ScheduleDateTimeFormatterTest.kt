package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDateTimeFormatterTest {
    @Test
    fun formatsDateTimeInRequestedZoneAndLocale() {
        val dateTime = ZonedDateTime.of(
            2026,
            1,
            2,
            10,
            0,
            0,
            0,
            ZoneId.of("UTC"),
        )

        val formatted = ScheduleDateTimeFormatter.formatForDisplay(
            dateTime = dateTime,
            zoneId = ZoneId.of("Asia/Tbilisi"),
            locale = Locale.US,
        )

        assertEquals("Jan 2, 2026, 2:00 PM", formatted)
    }
}
