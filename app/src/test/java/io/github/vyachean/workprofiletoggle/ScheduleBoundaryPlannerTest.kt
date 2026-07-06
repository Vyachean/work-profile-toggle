package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleBoundaryPlannerTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, zone)
    private val clock: Clock = Clock.fixed(now.toInstant(), zone)

    @Test
    fun schedulesNextBoundaryForReadySchedule() {
        val fixture = fixture()
        fixture.scheduleStore.save(readySchedule())

        val result = fixture.planner.refresh()

        val expectedBoundary = boundaryAt(hour = 17, expectedState = WorkProfileScheduleExpectedState.PAUSED)
        assertEquals(ScheduleBoundaryPlanResult.Scheduled(expectedBoundary), result)
        assertEquals(
            listOf(
                BackendCall.Cancel,
                BackendCall.SetExact(expectedBoundary.at.toInstant().toEpochMilli()),
            ),
            fixture.backend.calls,
        )
        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = now,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = false,
                nextBoundary = expectedBoundary,
                failureCategory = null,
            ),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun storesExactAlarmMissingResultWhenExactAlarmAccessIsMissing() {
        val fixture = fixture(canScheduleExactAlarms = false)
        fixture.scheduleStore.save(readySchedule())

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.EXACT_ALARM_ACCESS_MISSING),
            result,
        )
        assertEquals(emptyList<BackendCall>(), fixture.backend.calls)
        assertEquals(
            failedResult(ScheduleRuntimeFailureCategory.EXACT_ALARM_ACCESS_MISSING),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun cancelsAlarmAndStoresDisabledResultWhenScheduleIsDisabled() {
        val fixture = fixture()
        fixture.scheduleStore.save(readySchedule(enabled = false))

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Blocked(ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED),
            result,
        )
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun cancelsAlarmAndStoresIncompleteResultWhenScheduleIsIncomplete() {
        val fixture = fixture()
        fixture.scheduleStore.save(
            WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = null,
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Blocked(ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE),
            result,
        )
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_INCOMPLETE),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun cancelsAlarmAndStoresInvalidResultWhenScheduleIsInvalid() {
        val fixture = fixture()
        fixture.scheduleStore.save(
            WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = ScheduleTime(hour = 9, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Blocked(ScheduleRuntimeFailureCategory.SCHEDULE_INVALID),
            result,
        )
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_INVALID),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun storesAndroidRejectedResultWhenCancelBeforeSchedulingFails() {
        val fixture = fixture(cancelFailure = RuntimeException("Cancel rejected"))
        fixture.scheduleStore.save(readySchedule())

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            result,
        )
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            failedResult(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun storesAndroidRejectedResultWhenSchedulingFails() {
        val fixture = fixture(setExactFailure = RuntimeException("Alarm rejected"))
        fixture.scheduleStore.save(readySchedule())

        val result = fixture.planner.refresh()

        assertEquals(
            ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            result,
        )
        assertEquals(
            listOf(
                BackendCall.Cancel,
                BackendCall.SetExact(boundaryAt(hour = 17).at.toInstant().toEpochMilli()),
            ),
            fixture.backend.calls,
        )
        assertEquals(
            failedResult(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun cancelCancelsAlarmAndStoresDisabledResult() {
        val fixture = fixture()

        val result = fixture.planner.cancel()

        assertEquals(ScheduleBoundaryPlanResult.Cancelled, result)
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_DISABLED),
            fixture.runtimeResultStore.load(),
        )
    }

    @Test
    fun cancelStoresAndroidRejectedResultWhenCancelFails() {
        val fixture = fixture(cancelFailure = RuntimeException("Cancel rejected"))

        val result = fixture.planner.cancel()

        assertEquals(
            ScheduleBoundaryPlanResult.Failed(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            result,
        )
        assertEquals(listOf(BackendCall.Cancel), fixture.backend.calls)
        assertEquals(
            failedResult(ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED),
            fixture.runtimeResultStore.load(),
        )
    }

    private fun fixture(
        canScheduleExactAlarms: Boolean = true,
        cancelFailure: RuntimeException? = null,
        setExactFailure: RuntimeException? = null,
    ): Fixture {
        val keyValueStore = InMemoryKeyValueStore()
        val scheduleStore = WorkProfileScheduleStore(keyValueStore)
        val runtimeResultStore = ScheduleRuntimeResultStore(keyValueStore)
        val backend = FakeScheduleAlarmBackend(
            canScheduleExactAlarms = canScheduleExactAlarms,
            cancelFailure = cancelFailure,
            setExactFailure = setExactFailure,
        )
        val alarmScheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        return Fixture(
            scheduleStore = scheduleStore,
            runtimeResultStore = runtimeResultStore,
            backend = backend,
            planner = ScheduleBoundaryPlanner(
                scheduleStore = scheduleStore,
                alarmScheduler = alarmScheduler,
                runtimeResultStore = runtimeResultStore,
                clock = clock,
            ),
        )
    }

    private fun readySchedule(
        enabled: Boolean = true,
    ): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = enabled,
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            pauseAt = ScheduleTime(hour = 17, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY),
        )
    }

    private fun boundaryAt(
        hour: Int,
        expectedState: WorkProfileScheduleExpectedState = WorkProfileScheduleExpectedState.PAUSED,
    ): WorkProfileScheduleBoundary {
        return WorkProfileScheduleBoundary(
            at = ZonedDateTime.of(2026, 1, 5, hour, 0, 0, 0, zone),
            expectedState = expectedState,
        )
    }

    private fun blockedResult(
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

    private data class Fixture(
        val scheduleStore: WorkProfileScheduleStore,
        val runtimeResultStore: ScheduleRuntimeResultStore,
        val backend: FakeScheduleAlarmBackend,
        val planner: ScheduleBoundaryPlanner,
    )

    private class FakeScheduleAlarmBackend(
        private val canScheduleExactAlarms: Boolean = true,
        private val cancelFailure: RuntimeException? = null,
        private val setExactFailure: RuntimeException? = null,
    ) : ScheduleAlarmBackend {
        val calls = mutableListOf<BackendCall>()

        override fun canScheduleExactAlarms(): Boolean {
            return canScheduleExactAlarms
        }

        override fun setInexact(triggerAtEpochMillis: Long) {
            calls.add(BackendCall.SetInexact(triggerAtEpochMillis))
        }

        override fun setExact(triggerAtEpochMillis: Long) {
            calls.add(BackendCall.SetExact(triggerAtEpochMillis))
            setExactFailure?.let { failure -> throw failure }
        }

        override fun cancel() {
            calls.add(BackendCall.Cancel)
            cancelFailure?.let { failure -> throw failure }
        }
    }

    private sealed class BackendCall {
        data object Cancel : BackendCall()
        data class SetInexact(val triggerAtEpochMillis: Long) : BackendCall()
        data class SetExact(val triggerAtEpochMillis: Long) : BackendCall()
    }
}
