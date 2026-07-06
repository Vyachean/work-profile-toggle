package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleRuntimeDiagnosticsFormatterTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 10, 30, 0, 0, zone)

    @Test
    fun formatsCompleteRuntimeDiagnostics() {
        val result = ScheduleRuntimeDiagnosticsFormatter.format(
            appVersionName = "0.1.3",
            currentTime = now,
            schedule = WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY),
            ),
            exactAlarmAccessState = ScheduleExactAlarmAccessState.GRANTED,
            runtimeResult = ScheduleRuntimeResult(
                triggerTime = ZonedDateTime.of(2026, 1, 5, 9, 0, 0, 0, zone),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                nextBoundary = WorkProfileScheduleBoundary(
                    at = ZonedDateTime.of(2026, 1, 5, 17, 0, 0, 0, zone),
                    expectedState = WorkProfileScheduleExpectedState.PAUSED,
                ),
                failureCategory = null,
            ),
        )

        assertEquals(
            """
            Work Profile Toggle schedule diagnostics
            app.versionName=0.1.3
            current.time=2026-01-05T10:30Z[UTC]
            current.timezone=UTC
            schedule.enabled=true
            schedule.activeDays=MONDAY,WEDNESDAY
            schedule.resumeAt=09:00
            schedule.pauseAt=17:00
            exactAlarmAccess=GRANTED
            runtimeResult.present=true
            runtime.triggerTime=2026-01-05T09:00Z[UTC]
            runtime.expectedState=ACTIVE
            runtime.selectedProfileStatus=SELECTED
            runtime.requestedAction=ACTIVATE_WORK_PROFILE
            runtime.actionResult=SUCCEEDED
            runtime.finalStateConfirmed=true
            runtime.nextBoundary.at=2026-01-05T17:00Z[UTC]
            runtime.nextBoundary.expectedState=PAUSED
            runtime.failureCategory=null
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun formatsMissingRuntimeResultDiagnostics() {
        val result = ScheduleRuntimeDiagnosticsFormatter.format(
            appVersionName = "",
            currentTime = now,
            schedule = WorkProfileSchedule(
                enabled = true,
                resumeAt = null,
                pauseAt = ScheduleTime(hour = 17, minute = 0),
                activeDays = emptySet(),
            ),
            exactAlarmAccessState = ScheduleExactAlarmAccessState.MISSING,
            runtimeResult = null,
        )

        assertEquals(
            """
            Work Profile Toggle schedule diagnostics
            app.versionName=unknown
            current.time=2026-01-05T10:30Z[UTC]
            current.timezone=UTC
            schedule.enabled=true
            schedule.activeDays=none
            schedule.resumeAt=null
            schedule.pauseAt=17:00
            exactAlarmAccess=MISSING
            runtimeResult.present=false
            runtime.triggerTime=null
            runtime.expectedState=null
            runtime.selectedProfileStatus=null
            runtime.requestedAction=null
            runtime.actionResult=null
            runtime.finalStateConfirmed=null
            runtime.nextBoundary.at=null
            runtime.nextBoundary.expectedState=null
            runtime.failureCategory=null
            """.trimIndent(),
            result,
        )
    }
}
