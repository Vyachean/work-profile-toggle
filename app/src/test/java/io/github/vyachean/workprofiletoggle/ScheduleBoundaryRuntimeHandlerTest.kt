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
    fun savesReadyRuntimeResultWhenScheduleCanBeEvaluated() {
        val stores = stores()
        stores.scheduleStore.save(
            WorkProfileSchedule(
                enabled = true,
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                pauseAt = ScheduleTime(hour = 17, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )
        val handler = handler(stores)

        handler.handleBoundary()

        assertEquals(
            ScheduleRuntimeResult(
                triggerTime = triggerTime,
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = false,
                nextBoundary = WorkProfileScheduleBoundary(
                    at = ZonedDateTime.of(2026, 1, 5, 17, 0, 0, 0, zone),
                    expectedState = WorkProfileScheduleExpectedState.PAUSED,
                ),
                failureCategory = null,
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
        val handler = handler(stores)

        handler.handleBoundary()

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
        val handler = handler(stores)

        handler.handleBoundary()

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
        val handler = handler(stores)

        handler.handleBoundary()

        assertEquals(
            blockedResult(ScheduleRuntimeFailureCategory.SCHEDULE_INVALID),
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

    private fun handler(stores: Stores): ScheduleBoundaryRuntimeHandler {
        return ScheduleBoundaryRuntimeHandler(
            scheduleStore = stores.scheduleStore,
            runtimeResultStore = stores.runtimeResultStore,
            clock = Clock.fixed(triggerTime.toInstant(), zone),
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
}
