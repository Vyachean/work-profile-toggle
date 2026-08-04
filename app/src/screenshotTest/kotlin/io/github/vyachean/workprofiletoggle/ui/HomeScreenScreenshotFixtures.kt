package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Composable
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.HomeScheduleUiState
import io.github.vyachean.workprofiletoggle.HomeSetupState
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.ScheduleDay
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggle
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggleAction
import io.github.vyachean.workprofiletoggle.ScheduleEditorUiState
import io.github.vyachean.workprofiletoggle.ScheduleExactAlarmAccessState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextAction
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeStatusSummary
import io.github.vyachean.workprofiletoggle.ScheduleTime
import io.github.vyachean.workprofiletoggle.WorkProfileScheduleBoundary
import io.github.vyachean.workprofiletoggle.WorkProfileScheduleExpectedState
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
internal fun HomeScreenScreenshotFixture(state: HomeUiState) {
    WorkProfileToggleTheme {
        HomeScreenRoute(
            state = state,
            actions = NoOpHomeScreenActions,
        )
    }
}

internal object HomeScreenScreenshotStates {
    fun activeEnabled(): HomeUiState {
        return HomeUiState(
            primary = HomePrimaryState.WORK_PROFILE_ACTIVE,
            setup = HomeSetupState(
                ready = true,
                profileFound = true,
                selectedProfileLabel = "Work",
                permissionGranted = true,
            ),
            schedule = HomeScheduleUiState(
                configured = true,
                savedState = HomeScheduleSavedState.ENABLED,
                pauseAt = ScheduleTime(hour = 18, minute = 0),
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                activeDays = linkedSetOf(
                    ScheduleDay.MONDAY,
                    ScheduleDay.TUESDAY,
                    ScheduleDay.WEDNESDAY,
                    ScheduleDay.THURSDAY,
                    ScheduleDay.FRIDAY,
                ),
                exactAlarmAccessState = ScheduleExactAlarmAccessState.GRANTED,
                runtimeStatus = ScheduleRuntimeStatusSummary(
                    nextAction = ScheduleRuntimeNextAction(
                        type = ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE,
                        boundary = WorkProfileScheduleBoundary(
                            at = ZonedDateTime.of(
                                2026,
                                1,
                                2,
                                18,
                                0,
                                0,
                                0,
                                ZoneOffset.UTC,
                            ),
                            expectedState = WorkProfileScheduleExpectedState.PAUSED,
                        ),
                    ),
                    issue = null,
                ),
                canCopyDiagnostics = true,
                editor = ScheduleEditorUiState(
                    pauseInitialTime = ScheduleTime(hour = 18, minute = 0),
                    resumeInitialTime = ScheduleTime(hour = 9, minute = 0),
                    enableToggle = ScheduleEditorEnableToggle(
                        action = ScheduleEditorEnableToggleAction.DISABLE,
                    ),
                    showEnableRequirements = false,
                    canClear = true,
                ),
            ),
        )
    }
}
