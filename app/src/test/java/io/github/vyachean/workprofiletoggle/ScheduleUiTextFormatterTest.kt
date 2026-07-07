package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleUiTextFormatterTest {
    @Test
    fun formatsScheduleConfigurationText() {
        val formatter = formatter(
            strings = FakeScheduleStrings(
                unformattedValues = listOf(
                    "Saved schedule: Enabled",
                    "Exact alarm access: Missing",
                    "Not set",
                    "No days",
                    "Every day",
                    "Monday",
                    "Wednesday",
                ),
            ),
        )

        assertEquals("Saved schedule: Enabled", formatter.savedStateLabel(HomeScheduleSavedState.ENABLED))
        assertEquals("Exact alarm access: Missing", formatter.exactAlarmAccessLabel(ScheduleExactAlarmAccessState.MISSING))
        assertEquals("Not set", formatter.time(null))
        assertEquals("09:05", formatter.time(ScheduleTime(hour = 9, minute = 5)))
        assertEquals("No days", formatter.days(emptySet()))
        assertEquals("Every day", formatter.days(ScheduleDay.defaultSet))
        assertEquals(
            "Monday, Wednesday",
            formatter.days(setOf(ScheduleDay.WEDNESDAY, ScheduleDay.MONDAY)),
        )
    }

    @Test
    fun formatsRuntimeStatusText() {
        val formatter = formatter(
            strings = FakeScheduleStrings(
                unformattedValues = listOf("unlock required to resume work profile"),
                formattedValues = listOf(
                    "Next action: Pause work profile at Jan 2, 2026, 18:00",
                    "Schedule issue: unlock required to resume work profile",
                ),
            ),
        )
        val nextAction = ScheduleRuntimeNextAction(
            type = ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE,
            boundary = WorkProfileScheduleBoundary(
                at = ZonedDateTime.of(2026, 1, 2, 18, 0, 0, 0, ZoneId.of("UTC")),
                expectedState = WorkProfileScheduleExpectedState.PAUSED,
            ),
        )

        assertEquals(
            "Next action: Pause work profile at Jan 2, 2026, 18:00",
            formatter.nextAction(nextAction),
        )
        assertEquals(
            "Schedule issue: unlock required to resume work profile",
            formatter.runtimeIssue(ScheduleRuntimeIssue.CREDENTIAL_REQUIRED),
        )
    }

    private fun formatter(strings: ScheduleStringProvider): ScheduleUiTextFormatter {
        return ScheduleUiTextFormatter(
            strings = strings,
            timeFormatter = { time -> "%02d:%02d".format(Locale.ROOT, time.hour, time.minute) },
            dateTimeFormatter = { _ -> "Jan 2, 2026, 18:00" },
        )
    }

    private class FakeScheduleStrings(
        unformattedValues: List<String> = emptyList(),
        formattedValues: List<String> = emptyList(),
    ) : ScheduleStringProvider {
        private val unformattedQueue = ArrayDeque(unformattedValues)
        private val formattedQueue = ArrayDeque(formattedValues)

        override fun get(stringId: Int): String {
            return unformattedQueue.removeFirst()
        }

        override fun get(stringId: Int, vararg args: Any): String {
            return formattedQueue.removeFirst()
        }
    }
}
