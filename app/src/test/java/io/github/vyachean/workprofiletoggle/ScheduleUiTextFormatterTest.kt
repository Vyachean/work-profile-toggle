package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleUiTextFormatterTest {
    private val formatter = ScheduleUiTextFormatter(
        strings = object : ScheduleStringProvider {
            override fun get(stringId: Int): String = stringValue(stringId)
            override fun get(stringId: Int, vararg args: Any): String {
                return String.format(Locale.ROOT, stringValue(stringId), *args)
            }
        },
        timeFormatter = { time -> "%02d:%02d".format(Locale.ROOT, time.hour, time.minute) },
        dateTimeFormatter = { _ -> "Jan 2, 2026, 18:00" },
    )

    @Test
    fun formatsScheduleConfigurationText() {
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

    private companion object {
        private fun stringValue(stringId: Int): String {
            return requireNotNull(stringValues[stringId]) {
                "Missing test string for resource id $stringId"
            }
        }

        private val stringValues = mapOf(
            R.string.schedule_not_configured to "Not configured",
            R.string.schedule_saved_blocked_exact_alarm_access to "Saved schedule: Enabled, but exact alarm access is missing",
            R.string.schedule_saved_enabled to "Saved schedule: Enabled",
            R.string.schedule_saved_disabled to "Saved schedule: Disabled",
            R.string.schedule_exact_alarm_not_required to "Exact alarm access: Not required on this Android version",
            R.string.schedule_exact_alarm_granted to "Exact alarm access: Granted",
            R.string.schedule_exact_alarm_missing to "Exact alarm access: Missing",
            R.string.schedule_time_not_set to "Not set",
            R.string.schedule_all_days to "Every day",
            R.string.schedule_no_days to "No days",
            R.string.schedule_day_monday to "Monday",
            R.string.schedule_day_tuesday to "Tuesday",
            R.string.schedule_day_wednesday to "Wednesday",
            R.string.schedule_day_thursday to "Thursday",
            R.string.schedule_day_friday to "Friday",
            R.string.schedule_day_saturday to "Saturday",
            R.string.schedule_day_sunday to "Sunday",
            R.string.schedule_next_action_pause to "Next action: Pause work profile at %1$s",
            R.string.schedule_next_action_resume to "Next action: Resume work profile at %1$s",
            R.string.schedule_runtime_issue to "Schedule issue: %1$s",
            R.string.schedule_runtime_issue_pending to "waiting for the next schedule update",
            R.string.schedule_runtime_issue_disabled to "schedule disabled",
            R.string.schedule_runtime_issue_incomplete to "schedule incomplete",
            R.string.schedule_runtime_issue_invalid to "schedule invalid",
            R.string.schedule_runtime_issue_selected_profile_missing to "selected work profile missing",
            R.string.schedule_runtime_issue_work_profile_unavailable to "work profile unavailable",
            R.string.schedule_runtime_issue_permission_missing to "permission missing",
            R.string.schedule_runtime_issue_credential_required to "unlock required to resume work profile",
            R.string.schedule_runtime_issue_android_request_rejected to "Android rejected the request",
            R.string.schedule_runtime_issue_exact_alarm_access_missing to "alarm access missing",
            R.string.schedule_runtime_issue_runtime_exception to "runtime error",
        )
    }
}
