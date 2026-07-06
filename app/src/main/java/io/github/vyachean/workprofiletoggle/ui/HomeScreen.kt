package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.HomeScheduleUiState
import io.github.vyachean.workprofiletoggle.HomeSetupState
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggleAction
import io.github.vyachean.workprofiletoggle.ScheduleEditorUiState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeIssue
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeStatusSummary

internal data class HomeScreenActions(
    val onRefresh: () -> Unit = {},
    val onPauseWorkProfile: () -> Unit = {},
    val onResumeWorkProfile: () -> Unit = {},
    val onSelectWorkProfile: () -> Unit = {},
    val onChangeWorkProfile: () -> Unit = {},
    val onCopySetupCommand: () -> Unit = {},
    val onSetPauseTime: () -> Unit = {},
    val onSetResumeTime: () -> Unit = {},
    val onChooseActiveDays: () -> Unit = {},
    val onToggleSchedule: () -> Unit = {},
    val onClearSchedule: () -> Unit = {},
    val onCopyScheduleDiagnostics: () -> Unit = {},
    val onOpenExactAlarmSettings: () -> Unit = {},
)

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    actions: HomeScreenActions = HomeScreenActions(),
    modifier: Modifier = Modifier,
) {
    WorkProfileToggleTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Work Profile Toggle",
                    style = MaterialTheme.typography.headlineSmall,
                )
                PrimaryStatusCard(state.primary, actions)
                SetupCard(state.setup, actions)
                ScheduleCard(state.schedule, actions)
            }
        }
    }
}

@Composable
private fun PrimaryStatusCard(
    state: HomePrimaryState,
    actions: HomeScreenActions,
) {
    SectionCard(
        title = when (state) {
            HomePrimaryState.NO_WORK_PROFILE -> "No work profile found"
            HomePrimaryState.CHOOSE_WORK_PROFILE -> "Choose a work profile"
            HomePrimaryState.SETUP_REQUIRED -> "Setup required"
            HomePrimaryState.WORK_PROFILE_PAUSED -> "Work profile paused"
            HomePrimaryState.WORK_PROFILE_ACTIVE -> "Work profile active"
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> "Work profile status unknown"
        },
    ) {
        Text(
            text = when (state) {
                HomePrimaryState.NO_WORK_PROFILE -> "Create or enable a work profile, then check again."
                HomePrimaryState.CHOOSE_WORK_PROFILE -> "Select which work profile this app should control."
                HomePrimaryState.SETUP_REQUIRED -> "Grant the required quiet-mode permission before using controls."
                HomePrimaryState.WORK_PROFILE_PAUSED -> "Work apps are currently paused."
                HomePrimaryState.WORK_PROFILE_ACTIVE -> "Work apps are currently available."
                HomePrimaryState.WORK_PROFILE_UNKNOWN -> "The app could not read the current quiet-mode state."
            },
        )
        when (state) {
            HomePrimaryState.WORK_PROFILE_ACTIVE -> Button(onClick = actions.onPauseWorkProfile) {
                Text("Pause work profile")
            }
            HomePrimaryState.WORK_PROFILE_PAUSED -> Button(onClick = actions.onResumeWorkProfile) {
                Text("Resume work profile")
            }
            HomePrimaryState.CHOOSE_WORK_PROFILE -> Button(onClick = actions.onSelectWorkProfile) {
                Text("Choose profile")
            }
            HomePrimaryState.NO_WORK_PROFILE,
            HomePrimaryState.SETUP_REQUIRED,
            HomePrimaryState.WORK_PROFILE_UNKNOWN,
            -> OutlinedButton(onClick = actions.onRefresh) {
                Text("Check again")
            }
        }
    }
}

@Composable
private fun SetupCard(
    state: HomeSetupState,
    actions: HomeScreenActions,
) {
    SectionCard(title = "Setup") {
        StatusRow("Profile", if (state.profileFound) "Found" else "Missing")
        StatusRow("Selected profile", state.selectedProfileLabel ?: "None")
        StatusRow("Permission", if (state.permissionGranted) "Granted" else "Missing")
        Text(
            text = if (state.ready) {
                "Setup is complete."
            } else {
                "Complete setup before relying on schedule automation."
            },
        )
        if (!state.permissionGranted) {
            Button(onClick = actions.onCopySetupCommand) {
                Text("Copy setup command")
            }
        }
        if (state.selectedProfileLabel != null) {
            OutlinedButton(onClick = actions.onChangeWorkProfile) {
                Text("Change profile")
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    state: HomeScheduleUiState,
    actions: HomeScreenActions,
) {
    SectionCard(title = "Schedule") {
        StatusRow("Saved state", scheduleStateLabel(state.savedState))
        StatusRow("Exact alarm access", state.exactAlarmAccessState.name)
        if (!state.configured) {
            Text("Schedule is not configured yet.")
        }
        ScheduleRuntimeStatus(state.runtimeStatus)
        ScheduleEditorControls(state.editor, actions)
        if (state.savedState == HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS) {
            Button(onClick = actions.onOpenExactAlarmSettings) {
                Text("Open alarm settings")
            }
        }
        if (state.canCopyDiagnostics) {
            OutlinedButton(onClick = actions.onCopyScheduleDiagnostics) {
                Text("Copy diagnostics")
            }
        }
    }
}

@Composable
private fun ScheduleRuntimeStatus(status: ScheduleRuntimeStatusSummary?) {
    if (status == null) return

    status.nextAction?.let { nextAction ->
        Text(
            text = when (nextAction.type) {
                ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE -> "Next action: pause at ${nextAction.boundary.at}"
                ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE -> "Next action: resume at ${nextAction.boundary.at}"
            },
        )
    }
    status.issue?.let { issue ->
        Text("Runtime issue: ${issue.label()}")
    }
}

@Composable
private fun ScheduleEditorControls(
    state: ScheduleEditorUiState,
    actions: HomeScreenActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = actions.onSetPauseTime) {
            Text("Pause")
        }
        OutlinedButton(onClick = actions.onSetResumeTime) {
            Text("Resume")
        }
        OutlinedButton(onClick = actions.onChooseActiveDays) {
            Text("Days")
        }
    }
    state.enableToggle?.let { toggle ->
        Button(onClick = actions.onToggleSchedule) {
            Text(
                when (toggle.action) {
                    ScheduleEditorEnableToggleAction.ENABLE -> "Enable schedule"
                    ScheduleEditorEnableToggleAction.DISABLE -> "Disable schedule"
                },
            )
        }
    }
    if (state.showEnableRequirements) {
        Text("Set pause time, resume time, and active days before enabling the schedule.")
    }
    if (state.canClear) {
        OutlinedButton(onClick = actions.onClearSchedule) {
            Text("Clear schedule")
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(value)
    }
}

private fun scheduleStateLabel(state: HomeScheduleSavedState): String {
    return when (state) {
        HomeScheduleSavedState.NOT_CONFIGURED -> "Not configured"
        HomeScheduleSavedState.ENABLED -> "Enabled"
        HomeScheduleSavedState.DISABLED -> "Disabled"
        HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS -> "Blocked: exact alarm access missing"
    }
}

private fun ScheduleRuntimeIssue.label(): String {
    return when (this) {
        ScheduleRuntimeIssue.PENDING -> "Pending"
        ScheduleRuntimeIssue.SCHEDULE_DISABLED -> "Schedule disabled"
        ScheduleRuntimeIssue.SCHEDULE_INCOMPLETE -> "Schedule incomplete"
        ScheduleRuntimeIssue.SCHEDULE_INVALID -> "Schedule invalid"
        ScheduleRuntimeIssue.SELECTED_PROFILE_MISSING -> "Selected profile missing"
        ScheduleRuntimeIssue.WORK_PROFILE_UNAVAILABLE -> "Work profile unavailable"
        ScheduleRuntimeIssue.PERMISSION_MISSING -> "Permission missing"
        ScheduleRuntimeIssue.CREDENTIAL_REQUIRED -> "Credential required"
        ScheduleRuntimeIssue.ANDROID_REQUEST_REJECTED -> "Android request rejected"
        ScheduleRuntimeIssue.EXACT_ALARM_ACCESS_MISSING -> "Exact alarm access missing"
        ScheduleRuntimeIssue.RUNTIME_EXCEPTION -> "Runtime exception"
    }
}
