package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZonedDateTime

internal class ScheduleBoundaryPlanner(
    private val scheduleStore: WorkProfileScheduleStore,
    private val alarmScheduler: ScheduleAlarmScheduler,
    private val runtimeResultStore: ScheduleRuntimeResultStore,
    private val clock: Clock,
    private val precision: ScheduleAlarmPrecision = ScheduleAlarmPrecision.INEXACT,
) {
    fun refresh(): ScheduleBoundaryPlanResult {
        val now = ZonedDateTime.now(clock)
        return when (val calculation = WorkProfileScheduleCalculator.evaluate(scheduleStore.load(), now)) {
            is WorkProfileScheduleCalculation.Ready -> scheduleBoundary(
                calculation = calculation,
                now = now,
            )
            is WorkProfileScheduleCalculation.Blocked -> cancelBlockedSchedule(
                reason = calculation.reason,
                now = now,
            )
        }
    }

    fun cancel(): ScheduleBoundaryPlanResult {
        val now = ZonedDateTime.now(clock)
        return when (val result = alarmScheduler.cancel()) {
            ScheduleAlarmCancelResult.Cancelled -> {
                runtimeResultStore.save(blockedResult(now, ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED))
                ScheduleBoundaryPlanResult.Cancelled
            }
            is ScheduleAlarmCancelResult.Failed -> {
                runtimeResultStore.save(failedResult(now, result.reason.toFailureCategory()))
                ScheduleBoundaryPlanResult.Failed(result.reason.toFailureCategory())
            }
        }
    }

    private fun scheduleBoundary(
        calculation: WorkProfileScheduleCalculation.Ready,
        now: ZonedDateTime,
    ): ScheduleBoundaryPlanResult {
        val request = ScheduleAlarmRequest(
            triggerAt = calculation.nextBoundary.at,
            precision = precision,
        )
        return when (val result = alarmScheduler.schedule(request)) {
            is ScheduleAlarmScheduleResult.Scheduled -> {
                runtimeResultStore.save(
                    readyResult(
                        now = now,
                        expectedState = calculation.expectedState,
                        nextBoundary = calculation.nextBoundary,
                    ),
                )
                ScheduleBoundaryPlanResult.Scheduled(calculation.nextBoundary)
            }
            is ScheduleAlarmScheduleResult.Blocked -> {
                val failureCategory = result.reason.toFailureCategory()
                runtimeResultStore.save(failedResult(now, failureCategory))
                ScheduleBoundaryPlanResult.Failed(failureCategory)
            }
            is ScheduleAlarmScheduleResult.Failed -> {
                val failureCategory = result.reason.toFailureCategory()
                runtimeResultStore.save(failedResult(now, failureCategory))
                ScheduleBoundaryPlanResult.Failed(failureCategory)
            }
        }
    }

    private fun cancelBlockedSchedule(
        reason: WorkProfileScheduleBlockedReason,
        now: ZonedDateTime,
    ): ScheduleBoundaryPlanResult {
        val failureCategory = reason.toFailureCategory()
        return when (val cancelResult = alarmScheduler.cancel()) {
            ScheduleAlarmCancelResult.Cancelled -> {
                runtimeResultStore.save(blockedResult(now, failureCategory))
                ScheduleBoundaryPlanResult.Blocked(failureCategory)
            }
            is ScheduleAlarmCancelResult.Failed -> {
                val cancelFailureCategory = cancelResult.reason.toFailureCategory()
                runtimeResultStore.save(failedResult(now, cancelFailureCategory))
                ScheduleBoundaryPlanResult.Failed(cancelFailureCategory)
            }
        }
    }

    private fun readyResult(
        now: ZonedDateTime,
        expectedState: WorkProfileScheduleExpectedState,
        nextBoundary: WorkProfileScheduleBoundary,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = now,
            expectedState = expectedState,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
            finalStateConfirmed = false,
            nextBoundary = nextBoundary,
            failureCategory = null,
        )
    }

    private fun blockedResult(
        now: ZonedDateTime,
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = now,
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.BLOCKED,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = failureCategory,
        )
    }

    private fun failedResult(
        now: ZonedDateTime,
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = now,
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.FAILED,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = failureCategory,
        )
    }

    private fun WorkProfileScheduleBlockedReason.toFailureCategory(): ScheduleRuntimeFailureCategory {
        return when (this) {
            WorkProfileScheduleBlockedReason.SCHEDULE_DISABLED -> ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED
            WorkProfileScheduleBlockedReason.SCHEDULE_INCOMPLETE -> ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE
            WorkProfileScheduleBlockedReason.SCHEDULE_INVALID -> ScheduleRuntimeFailureCategory.SCHEDULE_INVALID
        }
    }

    private fun ScheduleAlarmBlockedReason.toFailureCategory(): ScheduleRuntimeFailureCategory {
        return when (this) {
            ScheduleAlarmBlockedReason.BOUNDARY_NOT_IN_FUTURE -> ScheduleRuntimeFailureCategory.SCHEDULE_INVALID
            ScheduleAlarmBlockedReason.EXACT_ALARM_ACCESS_MISSING -> ScheduleRuntimeFailureCategory.EXACT_ALARM_ACCESS_MISSING
        }
    }

    private fun ScheduleAlarmFailureReason.toFailureCategory(): ScheduleRuntimeFailureCategory {
        return when (this) {
            ScheduleAlarmFailureReason.ANDROID_ALARM_REJECTED -> ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED
        }
    }
}

internal sealed class ScheduleBoundaryPlanResult {
    data class Scheduled(
        val nextBoundary: WorkProfileScheduleBoundary,
    ) : ScheduleBoundaryPlanResult()

    data class Blocked(
        val failureCategory: ScheduleRuntimeFailureCategory,
    ) : ScheduleBoundaryPlanResult()

    data class Failed(
        val failureCategory: ScheduleRuntimeFailureCategory,
    ) : ScheduleBoundaryPlanResult()

    data object Cancelled : ScheduleBoundaryPlanResult()
}
