package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

internal interface ScheduleStringProvider {
    fun get(stringId: Int): String
    fun get(stringId: Int, vararg args: Any): String
}

internal class ScheduleUiTextFormatter(
    private val strings: ScheduleStringProvider,
    private val timeFormatter: (ScheduleTime) -> String,
    private val dateTimeFormatter: (ZonedDateTime) -> String,
) {
    fun savedStateLabel(state: HomeScheduleSavedState): String {
        return strings.get(savedStateLabelStringId(state))
    }

    fun exactAlarmAccessLabel(state: ScheduleExactAlarmAccessState): String {
        return strings.get(exactAlarmAccessLabelStringId(state))
    }

    fun time(scheduleTime: ScheduleTime?): String {
        return scheduleTime?.let(timeFormatter)
            ?: strings.get(R.string.schedule_time_not_set)
    }

    fun days(days: Set<ScheduleDay>): String {
        return when {
            days.isEmpty() -> strings.get(R.string.schedule_no_days)
            days == ScheduleDay.defaultSet -> strings.get(R.string.schedule_all_days)
            else -> days.sorted().joinToString(", ") { day -> dayLabel(day) }
        }
    }

    fun dayLabel(day: ScheduleDay): String {
        return strings.get(dayLabelStringId(day))
    }

    fun nextAction(nextAction: ScheduleRuntimeNextAction): String {
        val formattedBoundary = dateTimeFormatter(nextAction.boundary.at)
        return when (nextAction.type) {
            ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE -> strings.get(
                R.string.schedule_next_action_pause,
                formattedBoundary,
            )
            ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE -> strings.get(
                R.string.schedule_next_action_resume,
                formattedBoundary,
            )
        }
    }

    fun runtimeIssue(issue: ScheduleRuntimeIssue): String {
        return strings.get(
            R.string.schedule_runtime_issue,
            strings.get(runtimeIssueLabelStringId(issue)),
        )
    }

    private fun savedStateLabelStringId(state: HomeScheduleSavedState): Int {
        return when (state) {
            HomeScheduleSavedState.NOT_CONFIGURED -> R.string.schedule_not_configured
            HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS -> R.string.schedule_saved_blocked_exact_alarm_access
            HomeScheduleSavedState.ENABLED -> R.string.schedule_saved_enabled
            HomeScheduleSavedState.DISABLED -> R.string.schedule_saved_disabled
        }
    }

    private fun exactAlarmAccessLabelStringId(state: ScheduleExactAlarmAccessState): Int {
        return when (state) {
            ScheduleExactAlarmAccessState.NOT_REQUIRED -> R.string.schedule_exact_alarm_not_required
            ScheduleExactAlarmAccessState.GRANTED -> R.string.schedule_exact_alarm_granted
            ScheduleExactAlarmAccessState.MISSING -> R.string.schedule_exact_alarm_missing
        }
    }

    private fun dayLabelStringId(day: ScheduleDay): Int {
        return when (day) {
            ScheduleDay.MONDAY -> R.string.schedule_day_monday
            ScheduleDay.TUESDAY -> R.string.schedule_day_tuesday
            ScheduleDay.WEDNESDAY -> R.string.schedule_day_wednesday
            ScheduleDay.THURSDAY -> R.string.schedule_day_thursday
            ScheduleDay.FRIDAY -> R.string.schedule_day_friday
            ScheduleDay.SATURDAY -> R.string.schedule_day_saturday
            ScheduleDay.SUNDAY -> R.string.schedule_day_sunday
        }
    }

    private fun runtimeIssueLabelStringId(issue: ScheduleRuntimeIssue): Int {
        return when (issue) {
            ScheduleRuntimeIssue.PENDING -> R.string.schedule_runtime_issue_pending
            ScheduleRuntimeIssue.SCHEDULE_DISABLED -> R.string.schedule_runtime_issue_disabled
            ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE -> R.string.schedule_runtime_issue_incomplete
            ScheduleRuntimeIssue.SCHEDULE_INVALID -> R.string.schedule_runtime_issue_invalid
            ScheduleRuntimeIssue.SELECTED_PROFILE_MISSING -> R.string.schedule_runtime_issue_selected_profile_missing
            ScheduleRuntimeIssue.WORK_PROFILE_UNAVAILABLE -> R.string.schedule_runtime_issue_work_profile_unavailable
            ScheduleRuntimeIssue.PERMISSION_MISSING -> R.string.schedule_runtime_issue_permission_missing
            ScheduleRuntimeIssue.CREDENTIAL_REQUIRED -> R.string.schedule_runtime_issue_credential_required
            ScheduleRuntimeIssue.ANDROID_REQUEST_REJECTED -> R.string.schedule_runtime_issue_android_request_rejected
            ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING -> R.string.schedule_runtime_issue_exact_alarm_access_missing
            ScheduleRuntimeIssue.RUNTIME_EXCEPTION -> R.string.schedule_runtime_issue_runtime_exception
        }
    }
}
