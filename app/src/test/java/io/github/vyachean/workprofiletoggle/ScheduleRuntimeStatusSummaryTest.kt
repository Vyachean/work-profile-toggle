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
                now = now,
            ),
        )
    }

    @Test
    fun derivesNextActionWhenEnabledScheduleHasNoRuntimeResult() {
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
                result = null,
                now = now,
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
                now = now,
            ),
        )
    }

    @Test
    fun returnsResumeNextActionForActiveBoundary() {
        val morningBeforeResume = ZonedDateTime.of(2026, 1, 5, 8, 0, 0, 0, zone)
        val activeBoundary = WorkProfileScheduleBoundary(
            at = ZonedDateTime.of(2026, 1, 5, 9, 0, 0, 0, zone),
            expectedState = WorkProfileScheduleExpectedState.ACTIVE,
        )

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
                now = morningBeforeResume,
            ),
        )
    }

    @Test
    fun derivesFreshNextActionWhenRuntimeBoundaryIsStale() {
        val staleBoundary = WorkProfileScheduleBoundary(
            at = ZonedDateTime.of(2026, 1, 5, 9, 0, 0, 0, zone),
            expectedState = WorkProfileScheduleExpectedState.ACTIVE,
        )

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
                result = readyResult(nextBoundary = staleBoundary),
                now = now,
            ),
        )
    }

    @Test
    fun returnsFailureIssueWithDerivedNextActionWhenRuntimeResultHasFailure() {
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
                    nextBoundary = null,
                    failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
                ),
                now = now,
            ),
        )
    }

    @Test
    fun returnsScheduleIssueWhenScheduleIsIncomplete() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = null,
                issue = ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = WorkProfileSchedule(
                    enabled = true,
                    resumeAt = ScheduleTime(hour = 9, minute = 0),
                    pauseAt = null,
                    activeDays = setOf(ScheduleDay.MONDAY),
                ),
                result = null,
                now = now,
            ),
        )
    }

    @Test
    fun returnsScheduleIssueWhenScheduleIsInvalid() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = null,
                issue = ScheduleRuntimeIssue.SCHEDULE_INVALID,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = WorkProfileSchedule(
                    enabled = true,
                    resumeAt = ScheduleTime(hour = 9, minute = 0),
                    pauseAt = ScheduleTime(hour = 9, minute = 0),
                    activeDays = setOf(ScheduleDay.MONDAY),
                ),
                result = null,
                now = now,
            ),
        )
    }

    @Test
    fun returnsScheduleIssueBeforeStoredRuntimeFailureWhenScheduleIsInvalid() {
        assertEquals(
            ScheduleRuntimeStatusSummary(
                nextAction = null,
                issue = ScheduleRuntimeIssue.SCHEDULE_INVALID,
            ),
            ScheduleRuntimeStatusSummary.from(
                schedule = WorkProfileSchedule(
                    enabled = true,
                    resumeAt = ScheduleTime(hour = 9, minute = 0),
                    pauseAt = ScheduleTime(hour = 9, minute = 0),
                    activeDays = setOf(ScheduleDay.MONDAY),
                ),
                result = readyResult(
                    nextBoundary = null,
                    failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
                ),
                now = now,
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
