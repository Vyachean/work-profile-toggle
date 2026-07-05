package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZonedDateTime

internal class ScheduleBoundaryRuntimeHandler(
    private val scheduleStore: WorkProfileScheduleStore,
    private val runtimeResultStore: ScheduleRuntimeResultStore,
    private val workProfileReconciler: ScheduleWorkProfileReconciler,
    private val refreshBoundaryPlan: () -> ScheduleBoundaryPlanResult,
    private val clock: Clock,
) : ScheduleBoundaryHandler {
    override fun handleBoundary() {
        val triggerTime = ZonedDateTime.now(clock)
        when (val calculation = WorkProfileScheduleCalculator.evaluate(scheduleStore.load(), triggerTime)) {
            is WorkProfileScheduleCalculation.Ready -> handleReadyBoundary(
                triggerTime = triggerTime,
                calculation = calculation,
            )
            is WorkProfileScheduleCalculation.Blocked -> runtimeResultStore.save(
                calculation.toRuntimeResult(triggerTime),
            )
        }
    }

    override fun handleFailure(exception: Exception) {
        runtimeResultStore.save(
            ScheduleRuntimeResult(
                triggerTime = ZonedDateTime.now(clock),
                expectedState = null,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                finalStateConfirmed = false,
                nextBoundary = null,
                failureCategory = ScheduleRuntimeFailureCategory.RUNTIME_EXCEPTION,
            ),
        )
    }

    private fun handleReadyBoundary(
        triggerTime: ZonedDateTime,
        calculation: WorkProfileScheduleCalculation.Ready,
    ) {
        val reconciliation = workProfileReconciler.reconcile(calculation.expectedState)
        val planResult = refreshBoundaryPlan()
        runtimeResultStore.save(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = calculation.expectedState,
                selectedProfileStatus = reconciliation.selectedProfileStatus,
                requestedAction = reconciliation.requestedAction,
                actionResult = reconciliation.actionResult,
                finalStateConfirmed = reconciliation.finalStateConfirmed,
                nextBoundary = planResult.nextBoundaryOrNull(),
                failureCategory = reconciliation.failureCategory ?: planResult.failureCategoryOrNull(),
            ),
        )
    }

    private fun WorkProfileScheduleCalculation.Blocked.toRuntimeResult(
        triggerTime: ZonedDateTime,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
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

    private fun ScheduleBoundaryPlanResult.nextBoundaryOrNull(): WorkProfileScheduleBoundary? {
        return when (this) {
            is ScheduleBoundaryPlanResult.Scheduled -> nextBoundary
            is ScheduleBoundaryPlanResult.Blocked,
            is ScheduleBoundaryPlanResult.Failed,
            ScheduleBoundaryPlanResult.Cancelled,
            -> null
        }
    }

    private fun ScheduleBoundaryPlanResult.failureCategoryOrNull(): ScheduleRuntimeFailureCategory? {
        return when (this) {
            is ScheduleBoundaryPlanResult.Blocked -> failureCategory
            is ScheduleBoundaryPlanResult.Failed -> failureCategory
            is ScheduleBoundaryPlanResult.Scheduled,
            ScheduleBoundaryPlanResult.Cancelled,
            -> null
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
