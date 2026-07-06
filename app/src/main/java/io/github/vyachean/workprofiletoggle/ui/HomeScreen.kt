package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeScheduleSavedState
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggleAction
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeIssue
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeNextActionType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal data class HomeScreenActions(
    val onCheckAgain: () -> Unit,
    val onPauseWorkProfile: () -> Unit,
    val onResumeWorkProfile: () -> Unit,
    val onChangeProfile: () -> Unit,
    val onSetPauseTime: () -> Unit,
    val onSetResumeTime: () -> Unit,
    val onChooseActiveDays: () -> Unit,
    val onEnableSchedule: () -> Unit,
    val onDisableSchedule: () -> Unit,
    val onClearSchedule: () -> Unit,
    val onCopyDiagnostics: () -> Unit,
)

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
) {
    WorkProfileToggleTheme {
        Scaffold(modifier = modifier) { innerPadding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Work Profile Toggle",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PrimaryStatusCard(state = state, actions = actions)
                    SetupCard(state = state)
                    ScheduleCard(state = state, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun PrimaryStatusCard(
    state: HomeUiState,
    actions: HomeScreenActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = primaryTitle(state.primary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = primaryDescription(state.primary, state.setup.selectedProfileLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryActionRow(state = state.primary, actions = actions)
        }
    }
}

@Composable
private fun PrimaryActionRow(
    state: HomePrimaryState,
    actions: HomeScreenActions,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            HomePrimaryState.WORK_PROFILE_ACTIVE -> Button(onClick = actions.onPauseWorkProfile) {
                Text("Pause")
            }
            HomePrimaryState.WORK_PROFILE_PAUSED -> Button(onClick = actions.onResumeWorkProfile) {
                Text("Resume")
            }
            HomePrimaryState.CHOOSE_WORK_PROFILE -> Button(onClick = actions.onChangeProfile) {
                Text("Choose profile")
            }
            HomePrimaryState.NO_WORK_PROFILE,
            HomePrimaryState.SETUP_REQUIRED,
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> OutlinedButton(onClick = actions.onCheckAgain) {
                Text("Check again")
            }
        }
    }
}

@Composable
private fun SetupCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SetupRow(label = "Work profile", value = if (state.setup.profileFound) "Found" else "Missing")
            SetupRow(label = "Selected profile", value = state.setup.selectedProfileLabel ?: "Not selected")
            SetupRow(label = "Permission", value = if (state.setup.permissionGranted) "Granted" else "Missing")
        }
    }
}

@Composable
private fun SetupRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ScheduleCard(
    state: HomeUiState,
    actions: HomeScreenActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = scheduleStatusText(state.schedule.savedState),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.schedule.runtimeStatus?.nextAction?.let { nextAction ->
                val formattedBoundary = remember(nextAction.boundary.at) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.MEDIUM,
                        FormatStyle.SHORT,
                    ).withZone(ZoneId.systemDefault())
                    nextAction.boundary.at.format(formatter)
                }
                Text(
                    text = when (nextAction.type) {
                        ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE -> "Next action: pause at $formattedBoundary"
                        ScheduleRuntimeNextActionType.RESUME_WORK_PROFILE -> "Next action: resume at $formattedBoundary"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.schedule.runtimeStatus?.issue?.let { issue ->
                Text(
                    text = "Issue: ${scheduleIssueText(issue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ScheduleEditorActions(state = state, actions = actions)
        }
    }
}

@Composable
private fun ScheduleEditorActions(
    state: HomeUiState,
    actions: HomeScreenActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onSetPauseTime) {
                Text("Pause time")
            }
            OutlinedButton(onClick = actions.onSetResumeTime) {
                Text("Resume time")
            }
        }
        OutlinedButton(onClick = actions.onChooseActiveDays) {
            Text("Active days")
        }
        val enableToggle = state.schedule.editor.enableToggle
        if (enableToggle != null) {
            Button(
                onClick = when (enableToggle.action) {
                    ScheduleEditorEnableToggleAction.ENABLE -> actions.onEnableSchedule
                    ScheduleEditorEnableToggleAction.DISABLE -> actions.onDisableSchedule
                },
            ) {
                Text(
                    when (enableToggle.action) {
                        ScheduleEditorEnableToggleAction.ENABLE -> "Enable schedule"
                        ScheduleEditorEnableToggleAction.DISABLE -> "Disable schedule"
                    },
                )
            }
        } else if (state.schedule.editor.showEnableRequirements) {
            Text(
                text = "Set pause time, resume time, and active days before enabling the schedule.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.schedule.canCopyDiagnostics) {
            OutlinedButton(onClick = actions.onCopyDiagnostics) {
                Text("Copy diagnostics")
            }
        }
        if (state.schedule.editor.canClear) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = actions.onClearSchedule) {
                Text("Clear schedule")
            }
        }
    }
}

private fun primaryTitle(state: HomePrimaryState): String {
    return when (state) {
        HomePrimaryState.NO_WORK_PROFILE -> "No work profile found"
        HomePrimaryState.CHOOSE_WORK_PROFILE -> "Choose work profile"
        HomePrimaryState.SETUP_REQUIRED -> "Setup required"
        HomePrimaryState.WORK_PROFILE_PAUSED -> "Work profile paused"
        HomePrimaryState.WORK_PROFILE_ACTIVE -> "Work profile active"
        HomePrimaryState.WORK_PROFILE_UNKNOWN -> "Work profile status unknown"
    }
}

private fun primaryDescription(state: HomePrimaryState, selectedProfileLabel: String?): String {
    return when (state) {
        HomePrimaryState.NO_WORK_PROFILE -> "Create or enable a work profile, then check again."
        HomePrimaryState.CHOOSE_WORK_PROFILE -> "Select the profile that this app should control."
        HomePrimaryState.SETUP_REQUIRED -> "Grant quiet mode control permission before using manual actions or schedule."
        HomePrimaryState.WORK_PROFILE_PAUSED -> selectedProfileLabel?.let { "$it is paused." } ?: "The selected work profile is paused."
        HomePrimaryState.WORK_PROFILE_ACTIVE -> selectedProfileLabel?.let { "$it is active." } ?: "The selected work profile is active."
        HomePrimaryState.WORK_PROFILE_UNKNOWN -> "The app could not read the current quiet mode state."
    }
}

private fun scheduleStatusText(state: HomeScheduleSavedState): String {
    return when (state) {
        HomeScheduleSavedState.NOT_CONFIGURED -> "Schedule is not configured."
        HomeScheduleSavedState.ENABLED -> "Schedule is enabled."
        HomeScheduleSavedState.DISABLED -> "Schedule is saved but disabled."
        HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS -> "Schedule is blocked until exact alarm access is granted."
    }
}

private fun scheduleIssueText(issue: ScheduleRuntimeIssue): String {
    return when (issue) {
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
