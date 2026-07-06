package io.github.vyachean.workprofiletoggle

internal enum class ScheduleExactAlarmAccessState {
    NOT_REQUIRED,
    GRANTED,
    MISSING,
}

internal fun resolveScheduleExactAlarmAccess(
    sdkInt: Int,
    exactAlarmAccessIntroducedSdkInt: Int,
    canScheduleExactAlarms: () -> Boolean,
): ScheduleExactAlarmAccessState {
    return if (sdkInt < exactAlarmAccessIntroducedSdkInt) {
        ScheduleExactAlarmAccessState.NOT_REQUIRED
    } else if (canScheduleExactAlarms()) {
        ScheduleExactAlarmAccessState.GRANTED
    } else {
        ScheduleExactAlarmAccessState.MISSING
    }
}
