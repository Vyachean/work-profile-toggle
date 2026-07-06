package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

internal data class HomeUiState(
    val primary: HomePrimaryState,
    val setup: HomeSetupState,
    val schedule: HomeScheduleUiState,
)

internal enum class HomePrimaryState {
    NO_WORK_PROFILE,
    CHOOSE_WORK_PROFILE,
    SETUP_REQUIRED,
    WORK_PROFILE_PAUSED,
    WORK_PROFILE_ACTIVE,
    WORK_PROFILE_UNKNOWN,
}

internal data class HomeSetupState(
    val ready: Boolean,
    val profileFound: Boolean,
    val selectedProfileLabel: String?,
    val permissionGranted: Boolean,
)

internal data class HomeScheduleUiState(
    val configured: Boolean,
    val savedState: HomeScheduleSavedState,
    val exactAlarmAccessState: ScheduleExactAlarmAccessState,
    val runtimeStatus: ScheduleRuntimeStatusSummary?,
    val canCopyDiagnostics: Boolean,
    val editor: ScheduleEditorUiState,
)

internal enum class HomeScheduleSavedState {
    NOT_CONFIGURED,
    ENABLED,
    DISABLED,
    BLOCKED_EXACT_ALARM_ACCESS,
}

internal data class HomeUiStateInput(
    val profilesAvailable: Boolean,
    val availableProfileCount: Int,
    val selectedProfileLabel: String?,
    val selectedProfileQuietMode: Boolean?,
    val permissionGranted: Boolean,
    val schedule: WorkProfileSchedule,
    val exactAlarmAccessState: ScheduleExactAlarmAccessState,
    val scheduleRuntimeResult: ScheduleRuntimeResult?,
)

internal object HomeUiStateFactory {
    fun from(input: HomeUiStateInput, now: ZonedDateTime): HomeUiState {
        return HomeUiState(
            primary = primaryState(input),
            setup = setupState(input),
            schedule = scheduleState(input, now),
        )
    }

    private fun primaryState(input: HomeUiStateInput): HomePrimaryState {
        return when {
            !input.profilesAvailable || input.availableProfileCount == 0 -> HomePrimaryState.NO_WORK_PROFILE
            input.selectedProfileLabel == null -> HomePrimaryState.CHOOSE_WORK_PROFILE
            !input.permissionGranted -> HomePrimaryState.SETUP_REQUIRED
            input.selectedProfileQuietMode == true -> HomePrimaryState.WORK_PROFILE_PAUSED
            input.selectedProfileQuietMode == false -> HomePrimaryState.WORK_PROFILE_ACTIVE
            else -> HomePrimaryState.WORK_PROFILE_UNKNOWN
        }
    }

    private fun setupState(input: HomeUiStateInput): HomeSetupState {
        return HomeSetupState(
            ready = input.selectedProfileLabel != null && input.permissionGranted && input.profilesAvailable,
            profileFound = input.availableProfileCount > 0,
            selectedProfileLabel = input.selectedProfileLabel,
            permissionGranted = input.permissionGranted,
        )
    }

    private fun scheduleState(input: HomeUiStateInput, now: ZonedDateTime): HomeScheduleUiState {
        val configured = input.schedule != WorkProfileSchedule()
        return HomeScheduleUiState(
            configured = configured,
            savedState = savedState(input.schedule, input.exactAlarmAccessState, configured),
            exactAlarmAccessState = input.exactAlarmAccessState,
            runtimeStatus = ScheduleRuntimeStatusSummary.from(
                schedule = input.schedule,
                result = input.scheduleRuntimeResult,
                now = now,
            ),
            canCopyDiagnostics = configured,
            editor = ScheduleEditorUiStateFactory.from(input.schedule),
        )
    }

    private fun savedState(
        schedule: WorkProfileSchedule,
        exactAlarmAccessState: ScheduleExactAlarmAccessState,
        configured: Boolean,
    ): HomeScheduleSavedState {
        return when {
            !configured -> HomeScheduleSavedState.NOT_CONFIGURED
            schedule.enabled && exactAlarmAccessState == ScheduleExactAlarmAccessState.MISSING -> HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS
            schedule.enabled -> HomeScheduleSavedState.ENABLED
            else -> HomeScheduleSavedState.DISABLED
        }
    }
}
