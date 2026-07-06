package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

internal data class ScheduleRuntimeStatusSummary(
    val nextAction: ScheduleRuntimeNextAction?,
    val issue: ScheduleRuntimeIssue?,
) {
    companion object {
        fun from(
            schedule: WorkProfileSchedule,
            result: ScheduleRuntimeResult?,
            now: ZonedDateTime = ZonedDateTime.now(),
        ): ScheduleRuntimeStatusSummary? {
            if (!schedule.enabled) return null

            val calculation = WorkProfileScheduleCalculator.evaluate(schedule, now)
            val nextAction = calculation.nextBoundaryOrNull()?.toNextAction()
            return ScheduleRuntimeStatusSummary(
                nextAction = nextAction,
                issue = result?.failureCategory?.toRuntimeIssue()
                    ?: calculation.failureIssueOrNull()
                    ?: ScheduleRuntimeIssue.PENDING.takeIf { nextAction == null },
            )
        }
    }
}

internal data class ScheduleRuntimeNextAction(
    val type: ScheduleRuntimeNextActionType,
    val boundary: WorkProfileScheduleBoundary,
)

internal enum class ScheduleRuntimeNextActionType {
    PAUSE_WORK_PROFILE,
    RESUME_WORK_PROFILE,
}

internal enum class ScheduleRuntimeIssue {
    PENDING,
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

private fun WorkProfileScheduleCalculation.nextBoundaryOrNull(): WorkProfileScheduleBoundary? {
    return when (this) {
        is WorkProfileScheduleCalculation.Ready -> nextBoundary
        is WorkProfileScheduleCalculation.Blocked -> null
    }
}

private fun WorkProfileScheduleCalculation.failureIssueOrNull(): ScheduleRuntimeIssue? {
    return when (this) {
        is WorkProfileScheduleCalculation.Ready -> null
        is WorkProfileScheduleCalculation.Blocked -> reason.toRuntimeIssue()
    }
}

private fun WorkProfileScheduleBoundary.toNextAction(): ScheduleRuntimeNextAction {
    return ScheduleRuntimeNextAction(
        type = when (expectedState) {
            WorkProfileScheduleExpectedState.ACTIVE -> ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE
            WorkProfileScheduleExpectedState.PAUSED -> ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE
        },
        boundary = this,
    )
}

private fun WorkProfileScheduleBlockedReason.toRuntimeIssue(): ScheduleRuntimeIssue {
    return when (this) {
        WorkProfileScheduleBlockedReason.SCHEDULE_DISABLED -> ScheduleRuntimeIssue.SCHEDULE_DISABLED
        WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE -> ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE
        WorkProfileScheduleBlockedReason.SCHEDULE_INVALID -> ScheduleRuntimeIssue.SCHEDULE_INVALID
    }
}

private fun ScheduleRuntimeFailureCategory.toRuntimeIssue(): ScheduleRuntimeIssue {
    return when (this) {
        ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED -> ScheduleRuntimeIssue.SCHEDULE_DISABLED
        ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE -> ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE
        ScheduleRuntimeFailureCategory.SCHEDULE_INVALID -> ScheduleRuntimeIssue.SCHEDULE_INVALID
        ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING -> ScheduleRuntimeIssue.SELECTED_PROFILE_MISSING
        ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE -> ScheduleRuntimeIssue.WORK_PROFILE_UNAVAILABLE
        ScheduleRuntimeFailureCategory.PERMISSION_MISSING -> ScheduleRuntimeIssue.PERMISSION_MISSING
        ScheduleRuntimeFailureCategory.CREDENTIAL_REQUIRED -> ScheduleRuntimeIssue.CREDENTIAL_REQUIRED
        ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED -> ScheduleRuntimeIssue.ANDROID_REQUEST_REJECTED
        ScheduleRuntimeFailureCategory.EXACT_ALARM_ACCESS_MISSING -> ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING
        ScheduleRuntimeFailureCategory.RUNTIME_EXCEPTION -> ScheduleRuntimeIssue.RUNTIME_EXCEPTION
    }
}
