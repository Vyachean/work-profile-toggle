package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleRuntimeResultStoreTest {
    private val zone: ZoneId = ZoneId.of("UTC")

    @Test
    fun savesAndLoadsSuccessfulRuntimeResult() {
        val store = ScheduleRuntimeResultStore(InMemoryKeyValueStore())
        val result = ScheduleRuntimeResult(
            triggerTime = time(day = 5, hour = 9),
            expectedState = WorkProfileScheduleExpectedState.ACTIVE,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
            requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
            actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
            finalStateConfirmed = true,
            nextBoundary = WorkProfileScheduleBoundary(
                at = time(day = 5, hour = 17),
                expectedState = WorkProfileScheduleExpectedState.PAUSED,
            ),
            failureCategory = null,
        )

        store.save(result)

        assertEquals(result, store.load())
    }

    @Test
    fun savesAndLoadsBlockedRuntimeResultWithoutOptionalFields() {
        val store = ScheduleRuntimeResultStore(InMemoryKeyValueStore())
        val result = ScheduleRuntimeResult(
            triggerTime = time(day = 5, hour = 8),
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.BLOCKED,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
        )

        store.save(result)

        assertEquals(result, store.load())
    }

    @Test
    fun clearsOptionalFieldsWhenSavingBlockedRuntimeResultAfterSuccessfulResult() {
        val store = ScheduleRuntimeResultStore(InMemoryKeyValueStore())
        store.save(
            ScheduleRuntimeResult(
                triggerTime = time(day = 5, hour = 9),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                nextBoundary = WorkProfileScheduleBoundary(
                    at = time(day = 5, hour = 17),
                    expectedState = WorkProfileScheduleExpectedState.PAUSED,
                ),
                failureCategory = null,
            ),
        )
        val blockedResult = ScheduleRuntimeResult(
            triggerTime = time(day = 6, hour = 8),
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.BLOCKED,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
        )

        store.save(blockedResult)

        assertEquals(blockedResult, store.load())
    }

    @Test
    fun returnsNullWhenNoRuntimeResultIsSaved() {
        val store = ScheduleRuntimeResultStore(InMemoryKeyValueStore())

        assertNull(store.load())
    }

    @Test
    fun clearsSavedRuntimeResult() {
        val store = ScheduleRuntimeResultStore(InMemoryKeyValueStore())
        store.save(
            ScheduleRuntimeResult(
                triggerTime = time(day = 5, hour = 9),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = true,
                nextBoundary = null,
                failureCategory = null,
            ),
        )

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun returnsNullWhenRequiredFieldIsCorrupted() {
        val keyValueStore = InMemoryKeyValueStore()
        val store = ScheduleRuntimeResultStore(keyValueStore)
        store.save(
            ScheduleRuntimeResult(
                triggerTime = time(day = 5, hour = 9),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = true,
                nextBoundary = null,
                failureCategory = null,
            ),
        )
        keyValueStore.edit {
            putString("schedule_runtime_profile_status", "NOT_A_PROFILE_STATUS")
        }

        assertNull(store.load())
    }

    @Test
    fun returnsNullWhenOptionalEnumIsCorrupted() {
        val keyValueStore = InMemoryKeyValueStore()
        val store = ScheduleRuntimeResultStore(keyValueStore)
        store.save(
            ScheduleRuntimeResult(
                triggerTime = time(day = 5, hour = 9),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = true,
                nextBoundary = null,
                failureCategory = null,
            ),
        )
        keyValueStore.edit {
            putString("schedule_runtime_expected_state", "NOT_A_STATE")
        }

        assertNull(store.load())
    }

    @Test
    fun returnsNullWhenNextBoundaryIsPartial() {
        val keyValueStore = InMemoryKeyValueStore()
        val store = ScheduleRuntimeResultStore(keyValueStore)
        store.save(
            ScheduleRuntimeResult(
                triggerTime = time(day = 5, hour = 9),
                expectedState = WorkProfileScheduleExpectedState.ACTIVE,
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.NOT_REQUESTED,
                finalStateConfirmed = true,
                nextBoundary = WorkProfileScheduleBoundary(
                    at = time(day = 5, hour = 17),
                    expectedState = WorkProfileScheduleExpectedState.PAUSED,
                ),
                failureCategory = null,
            ),
        )
        keyValueStore.edit {
            remove("schedule_runtime_next_boundary_state")
        }

        assertNull(store.load())
    }

    private fun time(day: Int, hour: Int): ZonedDateTime {
        return ZonedDateTime.of(2026, 1, day, hour, 0, 0, 0, zone)
    }
}
