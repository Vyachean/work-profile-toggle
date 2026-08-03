package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
    PreviewHomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_ACTIVE,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.ENABLED,
            scheduleIssue = null,
            editorToggleAction = ScheduleEditorEnableToggleAction.DISABLE,
        ),
    )
}

@Preview(showBackground = true, name = "Paused profile")
@Composable
private fun HomeScreenPausedPreview() {
    PreviewHomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_PAUSED,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.DISABLED,
            scheduleIssue = null,
            editorToggleAction = ScheduleEditorEnableToggleAction.ENABLE,
        ),
    )
}

@Preview(showBackground = true, name = "Setup required")
@Composable
private fun HomeScreenSetupRequiredPreview() {
    PreviewHomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.SETUP_REQUIRED,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS,
            scheduleIssue = ScheduleRuntimeIssue.PERMISSION_MISSING,
            editorToggleAction = null,
            permissionGranted = false,
        ),
    )
}

@Preview(showBackground = true, name = "Next schedule action")
@Composable
private fun HomeScreenNextScheduleActionPreview() {
    PreviewHomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.WORK_PROFILE_ACTIVE,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.ENABLED,
            scheduleIssue = null,
            editorToggleAction = ScheduleEditorEnableToggleAction.DISABLE,
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
        ),
    )
}

@Preview(
    showBackground = true,
    name = "Compact setup required",
    widthDp = 320,
    heightDp = 480,
)
@Composable
private fun HomeScreenCompactSetupRequiredPreview() {
    PreviewHomeScreen(
        state = previewHomeState(
            primary = HomePrimaryState.SETUP_REQUIRED,
            profileLabel = "Work",
            scheduleSavedState = HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS,
            scheduleIssue = ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING,
            editorToggleAction = null,
            permissionGranted = false,
        ),
    )
}

@Composable
private fun PreviewHomeScreen(state: HomeUiState) {
    WorkProfileToggleTheme {
        HomeScreenRoute(
            state = state,
            actions = NoOpHomeScreenActions,
        )
    }
}

private fun previewHomeState(
    primary: HomePrimaryState,
    profileLabel: String?,
    scheduleSavedState: HomeScheduleSavedState,
    scheduleIssue: ScheduleRuntimeIssue?,
    editorToggleAction: ScheduleEditorEnableToggleAction?,
    permissionGranted: Boolean = true,
    nextAction: ScheduleRuntimeNextAction? = null,
): HomeUiState {
    val configured = scheduleSavedState != HomeScheduleSavedState.NOT_CONFIGURED
    return HomeUiState(
        primary = primary,
        setup = HomeSetupState(
            ready = profileLabel != null && permissionGranted,
            profileFound = true,
            selectedProfileLabel = profileLabel,
            permissionGranted = permissionGranted,
        ),
        schedule = HomeScheduleUiState(
            configured = configured,
            savedState = scheduleSavedState,
            pauseAt = if (configured) ScheduleTime(hour = 18, minute = 0) else null,
            resumeAt = if (configured) ScheduleTime(hour = 9, minute = 0) else null,
            activeDays = if (configured) {
                setOf(
                    ScheduleDay.MONDAY,
                    ScheduleDay.TUESDAY,
                    ScheduleDay.WEDNESDAY,
                    ScheduleDay.THURSDAY,
                    ScheduleDay.FRIDAY,
                )
            } else {
                emptySet()
            },
            exactAlarmAccessState = if (
                scheduleSavedState == HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS
            ) {
                ScheduleExactAlarmAccessState.MISSING
            } else {
                ScheduleExactAlarmAccessState.GRANTED
            },
            runtimeStatus = ScheduleRuntimeStatusSummary(
                nextAction = nextAction,
                issue = scheduleIssue,
            ),
            canCopyDiagnostics = configured,
            editor = ScheduleEditorUiState(
                pauseInitialTime = ScheduleTime(hour = 18, minute = 0),
                resumeInitialTime = ScheduleTime(hour = 9, minute = 0),
                enableToggle = editorToggleAction?.let { action ->
                    ScheduleEditorEnableToggle(action = action)
                },
                showEnableRequirements = editorToggleAction == null,
                canClear = configured,
            ),
        ),
    )
}
