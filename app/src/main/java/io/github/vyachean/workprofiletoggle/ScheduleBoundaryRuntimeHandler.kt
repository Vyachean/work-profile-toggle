package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZonedDateTime

internal class ScheduleBoundaryRuntimeHandler(
    private val scheduleStore: WorkProfileScheduleStore,
    private val runtimeResultStore: ScheduleRuntimeResultStore,
    private val clock: Clock,
) : ScheduleBoundaryHandler {
    override fun handleBoundary() {
        val triggerTime = ZonedDateTime.now(clock)
        val calculation = WorkProfileScheduleCalculator.evaluate(
            schedule = scheduleStore.load(),
            now = triggerTime,
        )

        runtimeResultStore.save(calculation.toRuntimeResult(triggerTime))
    }

    private fun WorkProfileScheduleCalculation.toRuntimeResult(
        triggerTime: ZonedDateTime,
    ): ScheduleRuntimeResult {
        return when (this) {
            is WorkProfileScheduleCalculation.Ready -> ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = expectedState,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = false,
                nextBoundary = nextBoundary,
                failureCategory = null,
            )
            is WorkProfileScheduleCalculation.Blocked -> ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = null,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                finalStateConfirmed = false,
                nextBoundary = null,
                failureCategory = reason.toFailureCategory(),
            )
        }
    }

    private fun WorkProfileScheduleBlockedReason.toFailureCategory(): ScheduleRuntimeFailureCategory {
        return when (this) {
            WorkProfileScheduleBlockedReason.SCHEDULE_DISABLED -> ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED
            WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE -> ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE
            WorkProfileScheduleBlockedReason.SCHEDULE_INVALID -> ScheduleRuntimeFailureCategory.SCHEDULE_INVALID
        }
    }
}
