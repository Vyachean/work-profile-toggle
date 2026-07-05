package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleBoundaryRuntimeHandlerTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val triggerTime: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, zone)

    @Test
    fun reconcilesSelectedProfileAndReschedulesNextBoundaryWhenScheduleCanBeEvaluated() {
        val stores = stores()
        stores.scheduleStore.save(readySchedule())
        val nextBoundary = boundaryAt(hour = 17, expectedState = WorkProfileScheduleExpectedState.PAUSED)
        val fixture = runtimeFixture(
            stores = stores,
            reconciliation = selectedNoopSuccess(),
            planResult = ScheduleBoundaryPlanResult.Scheduled(nextBoundary),
        )

        fixture.handler.handleBoundary()

        assertEquals(listOf(WorkProfileScheduleExpectedState.ACTIVE), fixture.reconciler.expectedStates)
        assertEquals(1, fixture.refreshCalls.size)
        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                nextBoundary = nextBoundary,
                failureCategory = null,
            ),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun storesReconciliationFailureAndStillRecordsNextBoundaryPlan() {
        val stores = stores()
        stores.scheduleStore.save(readySchedule())
        val nextBoundary = boundaryAt(hour = 17, expectedState = WorkProfileScheduleExpectedState.PAUSED)
        val fixture = runtimeFixture(
            stores = stores,
            reconciliation = ScheduleWorkProfileReconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                finalStateConfirmed = false,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            ),
            planResult = ScheduleBoundaryPlanResult.Scheduled(nextBoundary),
        )

        fixture.handler.handleBoundary()

        assertEquals(listOf(WorkProfileScheduleExpectedState.ACTIVE), fixture.reconciler.expectedStates)
        assertEquals(1, fixture.refreshCalls.size)
        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                finalStateConfirmed = false,
                nextBoundary = nextBoundary,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            ),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun storesRescheduleFailureWhenReconciliationSucceeds() {
        val stores = stores()
        stores.scheduleStore.save(readySchedule())
        val fixture = runtimeFixture(
            stores = stores,
            reconciliation = ScheduleWorkProfileReconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                failureCategory = null,
            ),
            planResult = ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
        )

        fixture.handler.handleBoundary()

        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                nextBoundary = null,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            ),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun keepsReconciliationFailureWhenRescheduleAlsoFails() {
        val stores = stores()
        stores.scheduleStore.save(readySchedule())
        val fixture = runtimeFixture(
            stores = stores,
            reconciliation = ScheduleWorkProfileReconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                finalStateConfirmed = false,
                failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
            ),
            planResult = ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
        )

        fixture.handler.handleBoundary()

        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                finalStateConfirmed = false,
                nextBoundary = null,
                failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
            ),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun savesDisabledRuntimeResultWhenScheduleIsDisabled() {
        val stores = stores()
        stores.scheduleStore.save(
            WorkProfileSchedule(
                enabled = false,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )
        val fixture = runtimeFixture(stores)

        fixture.handler.handleBoundary()

        assertEquals(emptyList<WorkProfileScheduleExpectedState>(), fixture.reconciler.expectedStates)
        assertEquals(0, fixture.refreshCalls.size)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun savesIncompleteRuntimeResultWhenScheduleIsIncomplete() {
        val stores = stores()
        stores.scheduleStore.save(
            WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = null,
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )
        val fixture = runtimeFixture(stores)

        fixture.handler.handleBoundary()

        assertEquals(emptyList<WorkProfileScheduleExpectedState>(), fixture.reconciler.expectedStates)
        assertEquals(0, fixture.refreshCalls.size)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun savesInvalidRuntimeResultWhenScheduleIsInvalid() {
        val stores = stores()
        stores.scheduleStore.save(
            WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = ScheduleTime(hour = 9, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )
        val fixture = runtimeFixture(stores)

        fixture.handler.handleBoundary()

        assertEquals(emptyList<WorkProfileScheduleExpectedState>(), fixture.reconciler.expectedStates)
        assertEquals(0, fixture.refreshCalls.size)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_INVALID),
            stores.runtimeResultStore.load(),
        )
    }

    @Test
    fun savesRuntimeExceptionResultWhenBoundaryHandlingFails() {
        val stores = stores()
        val fixture = runtimeFixture(stores)

        fixture.handler.handleFailure(Exception("Boundary handling failed"))

        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = null,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                finalStateConfirmed = false,
                nextBoundary = null,
                failureCategory = ScheduleRuntimeFailureCategory.RUNTIME_EXCEPTION,
            ),
            stores.runtimeResultStore.load(),
        )
    }

    private fun stores(): Stores {
        val keyValueStore = InMemoryKeyValueStore()
        return Stores(
            scheduleStore = WorkProfileScheduleStore(keyValueStore),
            runtimeResultStore = ScheduleRuntimeResultStore(keyValueStore),
        )
    }

    private fun runtimeFixture(
        stores: Stores,
        reconciliation: ScheduleWorkProfileReconciliation = selectedNoopSuccess(),
        planResult: ScheduleBoundaryPlanResult = ScheduleBoundaryPlanResult.Scheduled(
            boundaryAt(hour = 17, expectedState = WorkProfileScheduleExpectedState.PAUSED),
        ),
    ): RuntimeFixture {
        val reconciler = FakeScheduleWorkProfileReconciler(reconciliation)
        val refreshCalls = mutableListOf<Unit>()
        return RuntimeFixture(
            handler = ScheduleBoundaryRuntimeHandler(
                scheduleStore = stores.scheduleStore,
                runtimeResultStore = stores.runtimeResultStore,
                workProfileReconciler = reconciler,
                refreshBoundaryPlan = {
                    refreshCalls += Unit
                    planResult
                },
                clock = Clock.fixed(triggerTime.toInstant(), zone),
            ),
            reconciler = reconciler,
            refreshCalls = refreshCalls,
        )
    }

    private fun readySchedule(): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = true,
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            pauseAt = ScheduleTime(hour = 17, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY),
        )
    }

    private fun boundaryAt(
        hour: Int,
        expectedState: WorkProfileScheduleExpectedState,
    ): WorkProfileScheduleBoundary {
        return WorkProfileScheduleBoundary(
            at = ZonedDateTime.of(2026, 1, 5, hour, 0, 0, 0, zone),
            expectedState = expectedState,
        )
    }

    private fun selectedNoopSuccess(): ScheduleWorkProfileReconciliation {
        return ScheduleWorkProfileReconciliation(
            selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
            finalStateConfirmed = true,
            failureCategory = null,
        )
    }

    private fun blockedResult(
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = triggerTime,
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.BLOCKED,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = failureCategory,
        )
    }

    private data class Stores(
        val scheduleStore: WorkProfileScheduleStore,
        val runtimeResultStore: ScheduleRuntimeResultStore,
    )

    private data class RuntimeFixture(
        val handler: ScheduleBoundaryRuntimeHandler,
        val reconciler: FakeScheduleWorkProfileReconciler,
        val refreshCalls: MutableList<Unit>,
    )

    private class FakeScheduleWorkProfileReconciler(
        private val result: ScheduleWorkProfileReconciliation,
    ) : ScheduleWorkProfileReconciler {
        val expectedStates = mutableListOf<WorkProfileScheduleExpectedState>()

        override fun reconcile(expectedState: WorkProfileScheduleExpectedState): ScheduleWorkProfileReconciliation {
            expectedStates += expectedState
            return result
        }
    }
}
