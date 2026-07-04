package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

private const val PREF_SCHEDULE_RUNTIME_TRIGGER_TIME = "schedule_runtime_trigger_time"
private const val PREF_SCHEDULE_RUNTIME_EXPECTED_STATE = "schedule_runtime_expected_state"
private const val PREF_SCHEDULE_RUNTIME_PROFILE_STATUS = "schedule_runtime_profile_status"
private const val PREF_SCHEDULE_RUNTIME_REQUESTED_ACTION = "schedule_runtime_requested_action"
private const val PREF_SCHEDULE_RUNTIME_ACTION_RESULT = "schedule_runtime_action_result"
private const val PREF_SCHEDULE_RUNTIME_FINAL_STATE_CONFIRMED = "schedule_runtime_final_state_confirmed"
private const val PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_AT = "schedule_runtime_next_boundary_at"
private const val PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_STATE = "schedule_runtime_next_boundary_state"
private const val PREF_SCHEDULE_RUNTIME_FAILURE_CATEGORY = "schedule_runtime_failure_category"

internal class ScheduleRuntimeResultStore(
    private val keyValueStore: KeyValueStore,
) {
    fun load(): ScheduleRuntimeResult? {
        val triggerTime = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_TRIGGER_TIME)
            ?.let { value -> parseZonedDateTime(value) }
            ?: return null
        val profileStatus = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_PROFILE_STATUS)
            ?.let { value -> parseEnum<ScheduleRuntimeProfileStatus>(value) }
            ?: return null
        val requestedAction = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_REQUESTED_ACTION)
            ?.let { value -> parseEnum<ScheduleRuntimeRequestedAction>(value) }
            ?: return null
        val actionResult = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_ACTION_RESULT)
            ?.let { value -> parseEnum<ScheduleRuntimeActionResult>(value) }
            ?: return null
        val expectedState = parseOptionalEnum<WorkProfileScheduleExpectedState>(
            PREF_SCHEDULE_RUNTIME_EXPECTED_STATE,
        ) ?: return null
        val failureCategory = parseOptionalEnum<ScheduleRuntimeFailureCategory>(
            PREF_SCHEDULE_RUNTIME_FAILURE_CATEGORY,
        ) ?: return null
        val nextBoundary = loadNextBoundary() ?: return null

        return ScheduleRuntimeResult(
            triggerTime = triggerTime,
            expectedState = expectedState.value,
            selectedProfileStatus = profileStatus,
            requestedAction = requestedAction,
            actionResult = actionResult,
            finalStateConfirmed = keyValueStore.getBoolean(
                PREF_SCHEDULE_RUNTIME_FINAL_STATE_CONFIRMED,
                false,
            ),
            nextBoundary = nextBoundary.value,
            failureCategory = failureCategory.value,
        )
    }

    fun save(result: ScheduleRuntimeResult) {
        keyValueStore.edit {
            putString(PREF_SCHEDULE_RUNTIME_TRIGGER_TIME, result.triggerTime.toString())
            putString(PREF_SCHEDULE_RUNTIME_EXPECTED_STATE, result.expectedState?.name)
            putString(PREF_SCHEDULE_RUNTIME_PROFILE_STATUS, result.selectedProfileStatus.name)
            putString(PREF_SCHEDULE_RUNTIME_REQUESTED_ACTION, result.requestedAction.name)
            putString(PREF_SCHEDULE_RUNTIME_ACTION_RESULT, result.actionResult.name)
            putBoolean(PREF_SCHEDULE_RUNTIME_FINAL_STATE_CONFIRMED, result.finalStateConfirmed)
            putString(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_AT, result.nextBoundary?.at?.toString())
            putString(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_STATE, result.nextBoundary?.expectedState?.name)
            putString(PREF_SCHEDULE_RUNTIME_FAILURE_CATEGORY, result.failureCategory?.name)
        }
    }

    fun clear() {
        keyValueStore.edit {
            remove(PREF_SCHEDULE_RUNTIME_TRIGGER_TIME)
            remove(PREF_SCHEDULE_RUNTIME_EXPECTED_STATE)
            remove(PREF_SCHEDULE_RUNTIME_PROFILE_STATUS)
            remove(PREF_SCHEDULE_RUNTIME_REQUESTED_ACTION)
            remove(PREF_SCHEDULE_RUNTIME_ACTION_RESULT)
            remove(PREF_SCHEDULE_RUNTIME_FINAL_STATE_CONFIRMED)
            remove(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_AT)
            remove(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_STATE)
            remove(PREF_SCHEDULE_RUNTIME_FAILURE_CATEGORY)
        }
    }

    private fun loadNextBoundary(): OptionalValue<WorkProfileScheduleBoundary>? {
        val boundaryAtValue = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_AT)
        val boundaryStateValue = keyValueStore.getString(PREF_SCHEDULE_RUNTIME_NEXT_BOUNDARY_STATE)
        return when {
            boundaryAtValue == null && boundaryStateValue == null -> OptionalValue(null)
            boundaryAtValue == null || boundaryStateValue == null -> null
            else -> {
                val boundaryAt = parseZonedDateTime(boundaryAtValue) ?: return null
                val boundaryState = parseEnum<WorkProfileScheduleExpectedState>(boundaryStateValue)
                    ?: return null
                OptionalValue(
                    WorkProfileScheduleBoundary(
                        at = boundaryAt,
                        expectedState = boundaryState,
                    ),
                )
            }
        }
    }

    private inline fun <reified T : Enum<T>> parseOptionalEnum(key: String): OptionalValue<T>? {
        val value = keyValueStore.getString(key) ?: return OptionalValue(null)
        return OptionalValue(parseEnum<T>(value) ?: return null)
    }

    private fun parseZonedDateTime(value: String): ZonedDateTime? {
        return runCatching { ZonedDateTime.parse(value) }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String): T? {
        return enumValues<T>().firstOrNull { enumValue -> enumValue.name == value }
    }

    private data class OptionalValue<T>(val value: T?)
}
