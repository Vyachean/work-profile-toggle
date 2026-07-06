package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleExactAlarmAccessTest {
    @Test
    fun exactAlarmAccessIsNotRequiredBeforeAndroid12() {
        val result = resolveScheduleExactAlarmAccess(
            sdkInt = 30,
            exactAlarmAccessIntroducedSdkInt = 31,
            canScheduleExactAlarms = { false },
        )

        assertEquals(ScheduleExactAlarmAccessState.NOT_REQUIRED, result)
    }

    @Test
    fun exactAlarmAccessIsGrantedWhenAndroidAllowsExactAlarms() {
        val result = resolveScheduleExactAlarmAccess(
            sdkInt = 31,
            exactAlarmAccessIntroducedSdkInt = 31,
            canScheduleExactAlarms = { true },
        )

        assertEquals(ScheduleExactAlarmAccessState.GRANTED, result)
    }

    @Test
    fun exactAlarmAccessIsMissingWhenAndroidBlocksExactAlarms() {
        val result = resolveScheduleExactAlarmAccess(
            sdkInt = 31,
            exactAlarmAccessIntroducedSdkInt = 31,
            canScheduleExactAlarms = { false },
        )

        assertEquals(ScheduleExactAlarmAccessState.MISSING, result)
    }
}
