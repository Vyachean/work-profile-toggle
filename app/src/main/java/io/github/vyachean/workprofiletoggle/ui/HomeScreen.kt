package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vyachean.workprofiletoggle.HomePrimaryState
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.ScheduleDateTimeFormatter
import io.github.vyachean.workprofiletoggle.ScheduleEditorEnableToggleAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    eventHandler: HomeScreenEventHandler,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = HomeScreenText.appTitle())
                },
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrimaryStatusCard(state = state, eventHandler = eventHandler)
            SetupCard(state = state)
            ScheduleCard(state = state, eventHandler = eventHandler)
        }
    }
}

@Composable
private fun PrimaryStatusCard(
    state: HomeUiState,
    eventHandler: HomeScreenEventHandler,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = HomeScreenText.primaryTitle(state.primary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = HomeScreenText.primaryDescription(state.primary, state.setup.selectedProfileLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryActionRow(state = state.primary, eventHandler = eventHandler)
        }
    }
}

@Composable
private fun PrimaryActionRow(
    state: HomePrimaryState,
    eventHandler: HomeScreenEventHandler,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            HomePrimaryState.WORK_PROFILE_ACTIVE -> Button(
                onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.PauseWorkProfile) },
            ) {
                Text(HomeScreenText.pauseAction())
            }
            HomePrimaryState.WORK_PROFILE_PAUSED -> Button(
                onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.ResumeWorkProfile) },
            ) {
                Text(HomeScreenText.resumeAction())
            }
            HomePrimaryState.CHOOSE_WORK_PROFILE -> Button(
                onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.ChangeProfile) },
            ) {
                Text(HomeScreenText.chooseProfileAction())
            }
            HomePrimaryState.NO_WORK_PROFILE,
            HomePrimaryState.SETUP_REQUIRED,
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> OutlinedButton(
                onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.CheckAgain) },
            ) {
                Text(HomeScreenText.checkAgainAction())
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
                text = HomeScreenText.setupTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SetupRow(
                label = HomeScreenText.workProfileLabel(),
                value = if (state.setup.profileFound) HomeScreenText.foundValue() else HomeScreenText.missingValue(),
            )
            SetupRow(
                label = HomeScreenText.selectedProfileLabel(),
                value = state.setup.selectedProfileLabel ?: HomeScreenText.notSelectedValue(),
            )
            SetupRow(
                label = HomeScreenText.permissionLabel(),
                value = if (state.setup.permissionGranted) HomeScreenText.grantedValue() else HomeScreenText.missingValue(),
            )
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
    eventHandler: HomeScreenEventHandler,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = HomeScreenText.scheduleTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = HomeScreenText.scheduleStatus(state.schedule.savedState),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.schedule.runtimeStatus?.nextAction?.let { nextAction ->
                val configuration = LocalConfiguration.current
                val formattedBoundary = remember(nextAction.boundary.at, configuration) {
                    ScheduleDateTimeFormatter.formatForDisplay(nextAction.boundary.at)
                }
                Text(
                    text = HomeScreenText.nextAction(
                        type = nextAction.type,
                        formattedBoundary = formattedBoundary,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.schedule.runtimeStatus?.issue?.let { runtimeIssue ->
                Text(
                    text = HomeScreenText.formattedIssue(runtimeIssue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ScheduleEditorActions(state = state, eventHandler = eventHandler)
        }
    }
}

@Composable
private fun ScheduleEditorActions(
    state: HomeUiState,
    eventHandler: HomeScreenEventHandler,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.SetPauseTime) }) {
                Text(HomeScreenText.setPauseTimeAction())
            }
            OutlinedButton(onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.SetResumeTime) }) {
                Text(HomeScreenText.setResumeTimeAction())
            }
        }
        OutlinedButton(onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.ChooseActiveDays) }) {
            Text(HomeScreenText.chooseActiveDaysAction())
        }
        val enableToggle = state.schedule.editor.enableToggle
        if (enableToggle != null) {
            val event = when (enableToggle.action) {
                ScheduleEditorEnableToggleAction.ENABLE -> HomeScreenEvent.EnableSchedule
                ScheduleEditorEnableToggleAction.DISABLE -> HomeScreenEvent.DisableSchedule
            }
            Button(onClick = { eventHandler.onHomeScreenEvent(event) }) {
                Text(
                    when (enableToggle.action) {
                        ScheduleEditorEnableToggleAction.ENABLE -> HomeScreenText.enableScheduleAction()
                        ScheduleEditorEnableToggleAction.DISABLE -> HomeScreenText.disableScheduleAction()
                    },
                )
            }
        } else if (state.schedule.editor.showEnableRequirements) {
            Text(
                text = HomeScreenText.enableScheduleRequirements(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.schedule.canCopyDiagnostics) {
            OutlinedButton(onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.CopyDiagnostics) }) {
                Text(HomeScreenText.copyDiagnosticsAction())
            }
        }
        if (state.schedule.editor.canClear) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = { eventHandler.onHomeScreenEvent(HomeScreenEvent.ClearSchedule) }) {
                Text(HomeScreenText.clearScheduleAction())
            }
        }
    }
}
