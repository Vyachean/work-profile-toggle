package io.github.vyachean.workprofiletoggle.ui

import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeIssue
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType

// TODO: Move user-facing Home screen strings to Android string resources when Compose runtime wiring is ready.
internal object HomeScreenText {
    const val APP_TITLE = "Work Profile Toggle"
    const val SETUP_TITLE = "Setup"
    const val SCHEDULE_TITLE = "Schedule"

    const val PAUSE_ACTION = "Pause"
    const val RESUME_ACTION = "Resume"
    const val CHOOSE_PROFILE_ACTION = "Choose profile"
    const val CHECK_AGAIN_ACTION = "Check again"
    const val SET_PAUSE_TIME_ACTION = "Pause time"
    const val SET_RESUME_TIME_ACTION = "Resume time"
    const val CHOOSE_ACTIVE_DAYS_ACTION = "Active days"
    const val ENABLE_SCHEDULE_ACTION = "Enable schedule"
    const val DISABLE_SCHEDULE_ACTION = "Disable schedule"
    const val COPY_DIAGNOSTICS_ACTION = "Copy diagnostics"
    const val CLEAR_SCHEDULE_ACTION = "Clear schedule"

    const val WORK_PROFILE_LABEL = "Work profile"
    const val SELECTED_PROFILE_LABEL = "Selected profile"
    const val PERMISSION_LABEL = "Permission"
    const val FOUND_VALUE = "Found"
    const val MISSING_VALUE = "Missing"
    const val GRANTED_VALUE = "Granted"
    const val NOT_SELECTED_VALUE = "Not selected"

    const val ENABLE_SCHEDULE_REQUIREMENTS = "Set pause time, resume time, and active days before enabling the schedule."

    fun primaryTitle(state: HomePrimaryState): String {
        return when (state) {
            HomePrimaryState.NO_WORK_PROFILE -> "No work profile found"
            HomePrimaryState.CHOOSE_WORK_PROFILE -> "Choose work profile"
            HomePrimaryState.SETUP_REQUIRED -> "Setup required"
            HomePrimaryState.WORK_PROFILE_PAUSED -> "Work profile paused"
            HomePrimaryState.WORK_PROFILE_ACTIVE -> "Work profile active"
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> "Work profile status unknown"
        }
    }

    fun primaryDescription(state: HomePrimaryState, selectedProfileLabel: String?): String {
        return when (state) {
            HomePrimaryState.NO_WORK_PROFILE -> "Create or enable a work profile, then check again."
            HomePrimaryState.CHOOSE_WORK_PROFILE -> "Select the profile that this app should control."
            HomePrimaryState.SETUP_REQUIRED -> "Grant quiet mode control permission before using manual actions or schedule."
            HomePrimaryState.WORK_PROFILE_PAUSED -> selectedProfileLabel?.let { "$it is paused." } ?: "The selected work profile is paused."
            HomePrimaryState.WORK_PROFILE_ACTIVE -> selectedProfileLabel?.let { "$it is active." } ?: "The selected work profile is active."
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> "The app could not read the current quiet mode state."
        }
    }

    fun scheduleStatus(state: HomeScheduleSavedState): String {
        return when (state) {
            HomeScheduleSavedState.NOT_CONFIGURED -> "Schedule is not configured."
            HomeScheduleSavedState.ENABLED -> "Schedule is enabled."
            HomeScheduleSavedState.DISABLED -> "Schedule is saved but disabled."
            HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS -> "Schedule is blocked until exact alarm access is granted."
        }
    }

    fun nextAction(type: ScheduleRuntimeNextActionType, formattedBoundary: String): String {
        return when (type) {
            ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE -> "Next action: pause at $formattedBoundary"
            ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE -> "Next action: resume at $formattedBoundary"
        }
    }

    fun issue(issue: ScheduleRuntimeIssue): String {
        return when (issue) {
            ScheduleRuntimeIssue.PENDING -> "Pending"
            ScheduleRuntimeIssue.SCHEDULE_DISABLED -> "Schedule disabled"
            ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE -> "Schedule incomplete"
            ScheduleRuntimeIssue.SCHEDULE_INVALID -> "Schedule invalid"
            ScheduleRuntimeIssue.SELECTED_PROFILE_MISSING -> "Selected profile missing"
            ScheduleRuntimeIssue.WORK_PROFILE_UNAVAILABLE -> "Work profile unavailable"
            ScheduleRuntimeIssue.PERMISSION_MISSING -> "Permission missing"
            ScheduleRuntimeIssue.CREDENTIAL_REQUIRED -> "Credential required"
            ScheduleRuntimeIssue.ANDROID_REQUEST_REJECTED -> "Android request rejected"
            ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING -> "Exact alarm access missing"
            ScheduleRuntimeIssue.RUNTIME_EXCEPTION -> "Runtime exception"
        }
    }

    fun formattedIssue(issue: ScheduleRuntimeIssue): String {
        return "Issue: ${issue(issue)}"
    }
}
