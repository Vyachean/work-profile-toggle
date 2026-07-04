package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

internal data class ScheduleRuntimeResult(
    val triggerTime: ZonedDateTime,
    val expectedState: WorkProfileScheduleExpectedState?,
    val selectedProfileStatus: ScheduleRuntimeProfileStatus,
    val requestedAction: ScheduleRuntimeRequestedAction,
    val actionResult: ScheduleRuntimeActionResult,
    val finalStateConfirmed: Boolean,
    val nextBoundary: WorkProfileScheduleBoundary?,
    val failureCategory: ScheduleRuntimeFailureCategory?,
)

internal enum class ScheduleRuntimeProfileStatus {
    NOT_CHECKED,
    SELECTED,
    MISSING,
    UNAVAILABLE,
}

internal enum class ScheduleRuntimeRequestedAction {
    NONE,
    ACTIVATE_WORK_PROFILE,
    PAUSE_WORK_PROFILE,
}

internal enum class ScheduleRuntimeActionResult {
    NOT_REQUESTED,
    SUCCEEDED,
    FAILED,
    BLOCKED,
}

internal enum class ScheduleRuntimeFailureCategory {
    SCHEDULE_DISABLED,
    SCHEDULE_INCOMPLETE,
    SCHEDULE_INVALID,
    SELECTED_PROFILE_MISSING,
    WORK_PROFILE_UNAVAILABLE,
    PERMISSION_MISSING,
    CREDENTIAL_REQUIRED,
    ANDROID_REQUEST_REJECTED,
    EXACT_ALARM_ACCESS_MISSING,
    RUNTIME_EXCEPTION,
}
