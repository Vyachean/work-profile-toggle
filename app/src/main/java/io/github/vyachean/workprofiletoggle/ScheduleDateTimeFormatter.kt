package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal object ScheduleDateTimeFormatter {
    private val baseFormatter = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.MEDIUM,
        FormatStyle.SHORT,
    )

    fun formatForDisplay(
        dateTime: ZonedDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        return baseFormatter
            .withLocale(locale)
            .withZone(zoneId)
            .format(dateTime)
    }
}
