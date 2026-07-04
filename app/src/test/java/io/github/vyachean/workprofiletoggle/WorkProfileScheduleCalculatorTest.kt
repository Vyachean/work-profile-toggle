package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkProfileScheduleCalculatorTest {
    private val utc: ZoneId = ZoneId.of("UTC")

    @Test
    fun returnsActiveStateInsideSameDayWindow() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 10, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.ACTIVE, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 5,
                hour = 17,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.PAUSED,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun returnsPausedStateBeforeSameDayWindow() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 8, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.PAUSED, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 5,
                hour = 9,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun returnsNextActiveDayAfterSameDayWindowEnds() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 18, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.PAUSED, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 12,
                hour = 9,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun returnsActiveStateInsideOvernightWindowOnStartDay() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 22, minute = 0),
                workEnd = ScheduleTime(hour = 6, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 23, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.ACTIVE, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 6,
                hour = 6,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.PAUSED,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun treatsOvernightWorkDayAsWindowStartDay() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 22, minute = 0),
                workEnd = ScheduleTime(hour = 6, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 6, hour = 5, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.ACTIVE, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 6,
                hour = 6,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.PAUSED,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun doesNotTreatOvernightEndDayAsActiveWorkDay() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 22, minute = 0),
                workEnd = ScheduleTime(hour = 6, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 5, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.PAUSED, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 5,
                hour = 22,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun returnsPausedStateOnInactiveDay() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 6, hour = 10, minute = 0),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.PAUSED, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 12,
                hour = 9,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            ),
            result.nextBoundary,
        )
    }

    @Test
    fun returnsInvalidWhenStartTimeEqualsEndTime() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 9, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 8, minute = 0),
        )

        assertEquals(
            WorkProfileScheduleCalculation.Blocked(WorkProfileScheduleBlockedReason.SCHEDULE_INVALID),
            result,
        )
    }

    @Test
    fun returnsDisabledWhenScheduleIsDisabled() {
        val result = calculate(
            schedule = schedule(
                enabled = false,
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 8, minute = 0),
        )

        assertEquals(
            WorkProfileScheduleCalculation.Blocked(WorkProfileScheduleBlockedReason.SCHEDULE_DISABLED),
            result,
        )
    }

    @Test
    fun returnsIncompleteWhenTimeIsMissing() {
        val result = calculate(
            schedule = WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = null,
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 8, minute = 0),
        )

        assertEquals(
            WorkProfileScheduleCalculation.Blocked(WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE),
            result,
        )
    }

    @Test
    fun returnsIncompleteWhenActiveDaysAreEmpty() {
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = emptySet(),
            ),
            now = zoned(year = 2026, month = 1, day = 5, hour = 8, minute = 0),
        )

        assertEquals(
            WorkProfileScheduleCalculation.Blocked(WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE),
            result,
        )
    }

    @Test
    fun returnsBoundaryInCurrentDeviceTimezone() {
        val zone = ZoneId.of("Asia/Tbilisi")
        val result = calculate(
            schedule = schedule(
                workStart = ScheduleTime(hour = 9, minute = 0),
                workEnd = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
            now = zoned(year = 2026, month = 1, day = 4, hour = 20, minute = 0, zone = zone),
        ).asReady()

        assertEquals(WorkProfileScheduleExpectedState.PAUSED, result.expectedState)
        assertEquals(
            boundary(
                year = 2026,
                month = 1,
                day = 5,
                hour = 9,
                minute = 0,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                zone = zone,
            ),
            result.nextBoundary,
        )
    }

    private fun calculate(
        schedule: WorkProfileSchedule,
        now: ZonedDateTime,
    ): WorkProfileScheduleCalculation {
        return WorkProfileScheduleCalculator.evaluate(schedule = schedule, now = now)
    }

    private fun schedule(
        enabled: Boolean = true,
        workStart: ScheduleTime,
        workEnd: ScheduleTime,
        activeDays: Set<ScheduleDay>,
    ): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = enabled,
            resumeAt = workStart,
            pauseAt = workEnd,
            activeDays = activeDays,
        )
    }

    private fun WorkProfileScheduleCalculation.asReady(): WorkProfileScheduleCalculation.Ready {
        assertTrue(this is WorkProfileScheduleCalculation.Ready)
        return this as WorkProfileScheduleCalculation.Ready
    }

    private fun boundary(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        expectedState: WorkProfileScheduleExpectedState,
        zone: ZoneId = utc,
    ): WorkProfileScheduleBoundary {
        return WorkProfileScheduleBoundary(
            at = zoned(
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                zone = zone,
            ),
            expectedState = expectedState,
        )
    }

    private fun zoned(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zone: ZoneId = utc,
    ): ZonedDateTime {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
    }
}
