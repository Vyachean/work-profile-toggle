package io.github.vyachean.workprofiletoggle

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

internal object WorkProfileScheduleCalculator {
    private const val DAYS_TO_SEARCH = 14L

    fun evaluate(
        schedule: WorkProfileSchedule,
        now: ZonedDateTime,
    ): WorkProfileScheduleCalculation {
        val validation = validate(schedule)
        if (validation != null) return WorkProfileScheduleCalculation.Blocked(validation)

        val workStart = schedule.resumeAt!!.toLocalTime()
        val workEnd = schedule.pauseAt!!.toLocalTime()
        val activeDays = schedule.activeDays.map { day -> day.toDayOfWeek() }.toSet()

        val expectedState = if (isInsideActiveWindow(now, workStart, workEnd, activeDays)) {
            WorkProfileScheduleExpectedState.ACTIVE
        } else {
            WorkProfileScheduleExpectedState.PAUSED
        }

        val nextBoundary = requireNotNull(
            findNextBoundary(
                now = now,
                workStart = workStart,
                workEnd = workEnd,
                activeDays = activeDays,
            ),
        ) { "validated schedule must have a next boundary" }

        return WorkProfileScheduleCalculation.Ready(
            expectedState = expectedState,
            nextBoundary = nextBoundary,
        )
    }

    private fun validate(schedule: WorkProfileSchedule): WorkProfileScheduleBlockedReason? {
        if (!schedule.enabled) return WorkProfileScheduleBlockedReason.SCHEDULE_DISABLED
        if (schedule.activeDays.isEmpty()) return WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE
        if (schedule.resumeAt == null || schedule.pauseAt == null) {
            return WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE
        }
        if (schedule.resumeAt == schedule.pauseAt) return WorkProfileScheduleBlockedReason.SCHEDULE_INVALID
        return null
    }

    private fun isInsideActiveWindow(
        now: ZonedDateTime,
        workStart: LocalTime,
        workEnd: LocalTime,
        activeDays: Set<DayOfWeek>,
    ): Boolean {
        val zone = now.zone
        return candidateStartDates(now.toLocalDate()).any { startDate ->
            if (startDate.dayOfWeek in activeDays) {
                val start = activeWindowStart(startDate, workStart).atZone(zone)
                val end = activeWindowEnd(startDate, workStart, workEnd).atZone(zone)
                !now.isBefore(start) && now.isBefore(end)
            } else {
                false
            }
        }
    }

    private fun findNextBoundary(
        now: ZonedDateTime,
        workStart: LocalTime,
        workEnd: LocalTime,
        activeDays: Set<DayOfWeek>,
    ): WorkProfileScheduleBoundary? {
        val zone = now.zone
        return candidateStartDates(now.toLocalDate())
            .asSequence()
            .filter { startDate -> startDate.dayOfWeek in activeDays }
            .flatMap { startDate ->
                sequenceOf(
                    WorkProfileScheduleBoundary(
                        at = activeWindowStart(startDate, workStart).atZone(zone),
                        expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                    ),
                    WorkProfileScheduleBoundary(
                        at = activeWindowEnd(startDate, workStart, workEnd).atZone(zone),
                        expectedState = WorkProfileScheduleExpectedState.PAUSED,
                    ),
                )
            }
            .filter { boundary -> boundary.at.isAfter(now) }
            .minByOrNull { boundary -> boundary.at.toInstant() }
    }

    private fun candidateStartDates(today: LocalDate): List<LocalDate> {
        return (-1L..DAYS_TO_SEARCH).map { dayOffset -> today.plusDays(dayOffset) }
    }

    private fun activeWindowStart(
        startDate: LocalDate,
        workStart: LocalTime,
    ): LocalDateTime {
        return LocalDateTime.of(startDate, workStart)
    }

    private fun activeWindowEnd(
        startDate: LocalDate,
        workStart: LocalTime,
        workEnd: LocalTime,
    ): LocalDateTime {
        val endDate = if (workStart < workEnd) startDate else startDate.plusDays(1)
        return LocalDateTime.of(endDate, workEnd)
    }
}

internal sealed class WorkProfileScheduleCalculation {
    data class Ready(
        val expectedState: WorkProfileScheduleExpectedState,
        val nextBoundary: WorkProfileScheduleBoundary,
    ) : WorkProfileScheduleCalculation()

    data class Blocked(
        val reason: WorkProfileScheduleBlockedReason,
    ) : WorkProfileScheduleCalculation()
}

internal data class WorkProfileScheduleBoundary(
    val at: ZonedDateTime,
    val expectedState: WorkProfileScheduleExpectedState,
)

internal enum class WorkProfileScheduleExpectedState {
    ACTIVE,
    PAUSED,
}

internal enum class WorkProfileScheduleBlockedReason {
    SCHEDULE_DISABLED,
    SCHEDULE_INCOMPLETE,
    SCHEDULE_INVALID,
}

private fun ScheduleTime.toLocalTime(): LocalTime {
    return LocalTime.of(hour, minute)
}

private fun ScheduleDay.toDayOfWeek(): DayOfWeek {
    return when (this) {
        ScheduleDay.MONDAY -> DayOfWeek.MONDAY
        ScheduleDay.TUESDAY -> DayOfWeek.TUESDAY
        ScheduleDay.WEDNESDAY -> DayOfWeek.WEDNESDAY
        ScheduleDay.THURSDAY -> DayOfWeek.THURSDAY
        ScheduleDay.FRIDAY -> DayOfWeek.FRIDAY
        ScheduleDay.SATURDAY -> DayOfWeek.SATURDAY
        ScheduleDay.SUNDAY -> DayOfWeek.SUNDAY
    }
}
