package io.github.vyachean.workprofiletoggle.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.R
import io.github.vyachean.workprofiletoggle.ScheduleDay
import io.github.vyachean.workprofiletoggle.ScheduleExactAlarmAccessState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeIssue
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType
import io.github.vyachean.workprofiletoggle.ScheduleTime
import java.util.Calendar

internal object HomeScreenText {
    @Composable
    fun appTitle(): String = stringResource(R.string.app_name)

    @Composable
    fun setupTitle(): String = stringResource(R.string.setup_title)

    @Composable
    fun scheduleTitle(): String = stringResource(R.string.schedule_title)

    @Composable
    fun advancedTitle(): String = stringResource(R.string.advanced_title)

    @Composable
    fun diagnosticsAction(): String = stringResource(R.string.diagnostics_title)

    @Composable
    fun pauseAction(): String = stringResource(R.string.home_screen_pause_action)

    @Composable
    fun resumeAction(): String = stringResource(R.string.home_screen_resume_action)

    @Composable
    fun chooseProfileAction(): String = stringResource(R.string.home_screen_choose_profile_action)

    @Composable
    fun checkAgainAction(): String = stringResource(R.string.check_again)

    @Composable
    fun copySetupTextAction(): String = stringResource(R.string.copy_setup_text)

    @Composable
    fun setupPermissionDescription(): String = stringResource(R.string.adb_setup_description)

    @Composable
    fun setPauseTimeAction(): String = stringResource(R.string.home_screen_set_pause_time_action)

    @Composable
    fun setResumeTimeAction(): String = stringResource(R.string.home_screen_set_resume_time_action)

    @Composable
    fun chooseActiveDaysAction(): String = stringResource(R.string.schedule_choose_active_days)

    @Composable
    fun enableScheduleAction(): String = stringResource(R.string.home_screen_enable_schedule_action)

    @Composable
    fun disableScheduleAction(): String = stringResource(R.string.home_screen_disable_schedule_action)

    @Composable
    fun openExactAlarmSettingsAction(): String = stringResource(R.string.schedule_open_app_settings)

    @Composable
    fun exactAlarmAccessDescription(): String = stringResource(R.string.schedule_exact_alarm_missing_description)

    @Composable
    fun copyDiagnosticsAction(): String = stringResource(R.string.copy_schedule_diagnostics)

    @Composable
    fun clearScheduleAction(): String = stringResource(R.string.home_screen_clear_schedule_action)

    @Composable
    fun workProfileLabel(): String = stringResource(R.string.home_screen_work_profile_label)

    @Composable
    fun selectedProfileLabel(): String = stringResource(R.string.home_screen_selected_profile_label)

    @Composable
    fun permissionLabel(): String = stringResource(R.string.home_screen_permission_label)

    @Composable
    fun foundValue(): String = stringResource(R.string.home_screen_found_value)

    @Composable
    fun missingValue(): String = stringResource(R.string.home_screen_missing_value)

    @Composable
    fun grantedValue(): String = stringResource(R.string.home_screen_granted_value)

    @Composable
    fun notSelectedValue(): String = stringResource(R.string.home_screen_not_selected_value)

    @Composable
    fun enableScheduleRequirements(): String = stringResource(R.string.schedule_enable_requirements)

    @Composable
    fun scheduleStatusLabel(): String = stringResource(R.string.home_screen_schedule_status_label)

    @Composable
    fun pauseTimeLabel(): String = stringResource(R.string.home_screen_pause_time_label)

    @Composable
    fun resumeTimeLabel(): String = stringResource(R.string.home_screen_resume_time_label)

    @Composable
    fun activeDaysLabel(): String = stringResource(R.string.home_screen_active_days_label)

    @Composable
    fun nextActionLabel(): String = stringResource(R.string.home_screen_next_action_label)

    @Composable
    fun issueLabel(): String = stringResource(R.string.home_screen_issue_label)

    @Composable
    fun scheduleTime(time: ScheduleTime?): String {
        if (time == null) return stringResource(R.string.schedule_time_not_set)

        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        return remember(time, configuration, context) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            DateFormat.getTimeFormat(context).format(calendar.time)
        }
    }

    @Composable
    fun activeDays(days: Set<ScheduleDay>): String {
        if (days.isEmpty()) return stringResource(R.string.schedule_no_days)
        if (days.size == ScheduleDay.entries.size) return stringResource(R.string.schedule_all_days)

        val labels = buildList {
            if (ScheduleDay.MONDAY in days) add(stringResource(R.string.schedule_day_monday))
            if (ScheduleDay.TUESDAY in days) add(stringResource(R.string.schedule_day_tuesday))
            if (ScheduleDay.WEDNESDAY in days) add(stringResource(R.string.schedule_day_wednesday))
            if (ScheduleDay.THURSDAY in days) add(stringResource(R.string.schedule_day_thursday))
            if (ScheduleDay.FRIDAY in days) add(stringResource(R.string.schedule_day_friday))
            if (ScheduleDay.SATURDAY in days) add(stringResource(R.string.schedule_day_saturday))
            if (ScheduleDay.SUNDAY in days) add(stringResource(R.string.schedule_day_sunday))
        }
        return labels.joinToString(", ")
    }

    @Composable
    fun exactAlarmAccess(state: ScheduleExactAlarmAccessState): String {
        return stringResource(
            when (state) {
                ScheduleExactAlarmAccessState.GRANTED -> R.string.schedule_exact_alarm_granted
                ScheduleExactAlarmAccessState.MISSING -> R.string.schedule_exact_alarm_missing
                ScheduleExactAlarmAccessState.NOT_REQUIRED -> R.string.schedule_exact_alarm_not_required
            },
        )
    }

    @Composable
    fun primaryTitle(state: HomePrimaryState): String {
        return stringResource(
            when (state) {
                HomePrimaryState.NO_WORK_PROFILE -> R.string.home_screen_no_work_profile_title
                HomePrimaryState.CHOOSE_WORK_PROFILE -> R.string.home_screen_choose_work_profile_title
                HomePrimaryState.SETUP_REQUIRED -> R.string.setup_required
                HomePrimaryState.WORK_PROFILE_PAUSED -> R.string.home_screen_work_profile_paused_title
                HomePrimaryState.WORK_PROFILE_ACTIVE -> R.string.home_screen_work_profile_active_title
                HomePrimaryState.WORK_PROFILE_UNKNOWN -> R.string.home_screen_work_profile_unknown_title
            },
        )
    }

    @Composable
    fun primaryDescription(state: HomePrimaryState, selectedProfileLabel: String?): String {
        return when (state) {
            HomePrimaryState.NO_WORK_PROFILE -> stringResource(R.string.home_screen_no_work_profile_description)
            HomePrimaryState.CHOOSE_WORK_PROFILE -> stringResource(R.string.home_screen_choose_work_profile_description)
            HomePrimaryState.SETUP_REQUIRED -> stringResource(R.string.home_screen_setup_required_description)
            HomePrimaryState.WORK_PROFILE_PAUSED -> selectedProfileLabel?.let { label ->
                stringResource(R.string.home_screen_work_profile_paused_labeled_description, label)
            } ?: stringResource(R.string.home_screen_work_profile_paused_description)
            HomePrimaryState.WORK_PROFILE_ACTIVE -> selectedProfileLabel?.let { label ->
                stringResource(R.string.home_screen_work_profile_active_labeled_description, label)
            } ?: stringResource(R.string.home_screen_work_profile_active_description)
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> stringResource(R.string.home_screen_work_profile_unknown_description)
        }
    }

    @Composable
    fun scheduleStatus(state: HomeScheduleSavedState): String {
        return stringResource(
            when (state) {
                HomeScheduleSavedState.NOT_CONFIGURED -> R.string.home_screen_schedule_not_configured
                HomeScheduleSavedState.ENABLED -> R.string.home_screen_schedule_enabled
                HomeScheduleSavedState.DISABLED -> R.string.home_screen_schedule_disabled
                HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS -> R.string.home_screen_schedule_blocked_exact_alarm_access
            },
        )
    }

    @Composable
    fun nextActionValue(type: ScheduleRuntimeNextActionType, formattedBoundary: String): String {
        return stringResource(
            when (type) {
                ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE -> R.string.home_screen_next_action_pause_value
                ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE -> R.string.home_screen_next_action_resume_value
            },
            formattedBoundary,
        )
    }

    @Composable
    fun issue(issue: ScheduleRuntimeIssue): String {
        return stringResource(
            when (issue) {
                ScheduleRuntimeIssue.PENDING -> R.string.home_screen_issue_pending
                ScheduleRuntimeIssue.SCHEDULE_DISABLED -> R.string.home_screen_issue_schedule_disabled
                ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE -> R.string.home_screen_issue_schedule_incomplete
                ScheduleRuntimeIssue.SCHEDULE_INVALID -> R.string.home_screen_issue_schedule_invalid
                ScheduleRuntimeIssue.SELECTED_PROFILE_MISSING -> R.string.home_screen_issue_selected_profile_missing
                ScheduleRuntimeIssue.WORK_PROFILE_UNAVAILABLE -> R.string.home_screen_issue_work_profile_unavailable
                ScheduleRuntimeIssue.PERMISSION_MISSING -> R.string.home_screen_issue_permission_missing
                ScheduleRuntimeIssue.CREDENTIAL_REQUIRED -> R.string.home_screen_issue_credential_required
                ScheduleRuntimeIssue.ANDROID_REQUEST_REJECTED -> R.string.home_screen_issue_android_request_rejected
                ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING -> R.string.home_screen_issue_exact_alarm_access_missing
                ScheduleRuntimeIssue.RUNTIME_EXCEPTION -> R.string.home_screen_issue_runtime_exception
            },
        )
    }
}
