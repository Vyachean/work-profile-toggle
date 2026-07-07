package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.HomeScheduleUiState
import io.github.vyachean.workprofiletoggle.HomeSetupState
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggle
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggleAction
import io.github.vyachean.workprofiletoggle.ScheduleEditorUiState
import io.github.vyachean.workprofiletoggle.ScheduleExactAlarmAccessState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeIssue
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextAction
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeStatusSummary
import io.github.vyachean.workprofiletoggle.ScheduleTime
import io.github.vyachean.workprofiletoggle.WorkProfileScheduleBoundary
import io.github.vyachean.workprofiletoggle.WorkProfileScheduleExpectedState
import java.time.ZoneId
import java.time.ZonedDateTime

@Preview(showBackground = true, name = "Active profile")
@Composable
private fun HomeScreenActivePreview() {
    HomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_ACTIVE,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.ENABLED,
            scheduleIssue = null,
            editorToggleAction = ScheduleEditorEnableToggleAction.DISABLE,
        ),
        actions = previewActions,
    )
}

@Preview(showBackground = true, name = "Paused profile")
@Composable
private fun HomeScreenPausedPreview() {
    HomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_PAUSED,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.DISABLED,
            scheduleIssue = null,
            editorToggleAction = ScheduleEditorEnableToggleAction.ENABLE,
        ),
        actions = previewActions,
    )
}

@Preview(showBackground = true, name = "Setup required")
@Composable
private fun HomeScreenSetupRequiredPreview() {
    HomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.SETUP_REQUIRED,
            profileLabel = "Work",
            permissionGranted = false,
            scheduleSavedState = HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS,
            scheduleIssue = ScheduleRuntimeIssue.PERMISSION_MISSING,
            editorToggleAction = null,
        ),
        actions = previewActions,
    )
}

@Preview(showBackground = true, name = "Next schedule action")
@Composable
private fun HomeScreenNextScheduleActionPreview() {
    HomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_ACTIVE,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.ENABLED,
            scheduleIssue = null,
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
                        ZoneId.of("Asia/Tbilisi"),
                    ),
                    expectedState = WorkProfileScheduleExpectedState.PAUSED,
                ),
            ),
            editorToggleAction = ScheduleEditorEnableToggleAction.DISABLE,
        ),
        actions = previewActions,
    )
}

private fun previewHomeState(
    primary: HomePrimaryState,
    profileLabel: String?,
    permissionGranted: Boolean = true,
    scheduleSavedState: HomeScheduleSavedState,
    scheduleIssue: ScheduleRuntimeIssue?,
    nextAction: ScheduleRuntimeNextAction? = null,
    editorToggleAction: ScheduleEditorEnableToggleAction?,
): HomeUiState {
    return HomeUiState(
        primary = primary,
        setup = HomeSetupState(
            ready = profileLabel != null && permissionGranted,
            profileFound = true,
            selectedProfileLabel = profileLabel,
            permissionGranted = permissionGranted,
        ),
        schedule = HomeScheduleUiState(
            configured = scheduleSavedState != HomeScheduleSavedState.NOT_CONFIGURED,
            savedState = scheduleSavedState,
            exactAlarmAccessState = ScheduleExactAlarmAccessState.GRANTED,
            runtimeStatus = ScheduleRuntimeStatusSummary(
                nextAction = nextAction,
                issue = scheduleIssue,
            ),
            canCopyDiagnostics = scheduleSavedState != HomeScheduleSavedState.NOT_CONFIGURED,
            editor = ScheduleEditorUiState(
                pauseInitialTime = ScheduleTime(hour = 18, minute = 0),
                resumeInitialTime = ScheduleTime(hour = 9, minute = 0),
                enableToggle = editorToggleAction?.let { action ->
                    ScheduleEditorEnableToggle(action = action)
                },
                showEnableRequirements = editorToggleAction == null,
                canClear = scheduleSavedState != HomeScheduleSavedState.NOT_CONFIGURED,
            ),
        ),
    )
}

private val previewActions = HomeScreenActions(
    onCheckAgain = {},
    onPauseWorkProfile = {},
    onResumeWorkProfile = {},
    onChangeProfile = {},
    onSetPauseTime = {},
    onSetResumeTime = {},
    onChooseActiveDays = {},
    onEnableSchedule = {},
    onDisableSchedule = {},
    onClearSchedule = {},
    onCopyDiagnostics = {},
)
