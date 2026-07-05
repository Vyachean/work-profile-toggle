package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleRuntimeStatusSummaryTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, zone)
    private val nextBoundary: WorkProfileScheduleBoundary = WorkProfileScheduleBoundary(
        at = ZonedDateTime.of(2026, 1, 5, 17, 0, 0, 0, zone),
        expectedState = WorkProfileScheduleExpectedState.PAUSED,
    )

    @Test
    fun returnsNullForDisabledSchedule() {
        assertNull(
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(enabled = false),
                result = readyResult(nextBoundary = nextBoundary),
            ),
        )
    }

    @Test
    fun returnsPendingIssueWhenEnabledScheduleHasNoRuntimeResult() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = null,
                issue = ScheduleRuntimeIssue.PENDING,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(),
                result = null,
            ),
        )
    }

    @Test
    fun returnsPauseNextActionForPausedBoundary() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = ScheduleRuntimeNextAction(
                    type = ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE,
                    boundary = nextBoundary,
                ),
                issue = null,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(),
                result = readyResult(nextBoundary = nextBoundary),
            ),
        )
    }

    @Test
    fun returnsResumeNextActionForActiveBoundary() {
        val activeBoundary = nextBoundary.copy(expectedState = WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = ScheduleRuntimeNextAction(
                    type = ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE,
                    boundary = activeBoundary,
                ),
                issue = null,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(),
                result = readyResult(nextBoundary = activeBoundary),
            ),
        )
    }

    @Test
    fun returnsFailureIssueWithNextActionWhenRuntimeResultHasFailureAndBoundary() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = ScheduleRuntimeNextAction(
                    type = ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE,
                    boundary = nextBoundary,
                ),
                issue = ScheduleRuntimeIssue.PERMISSION_MISSING,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(),
                result = readyResult(
                    nextBoundary = nextBoundary,
                    failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
                ),
            ),
        )
    }

    @Test
    fun returnsPendingIssueWhenEnabledRuntimeResultHasNoBoundaryOrFailure() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = null,
                issue = ScheduleRuntimeIssue.PENDING,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = readySchedule(),
                result = readyResult(nextBoundary = null),
            ),
        )
    }

    private fun readySchedule(enabled: Boolean = true): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = enabled,
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            pauseAt = ScheduleTime(hour = 17, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY),
        )
    }

    private fun readyResult(
        nextBoundary: WorkProfileScheduleBoundary?,
        failureCategory: ScheduleRuntimeFailureCategory? = null,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = now,
            expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
            finalStateConfirmed = true,
            nextBoundary = nextBoundary,
            failureCategory = failureCategory,
        )
    }
}
