package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Composable
import io.github.vyachean.workprofiletoggle.HomeUiState
import io.github.vyachean.workprofiletoggle.HomeUiStateFactory
import io.github.vyachean.workprofiletoggle.HomeUiStateInput
import io.github.vyachean.workprofiletoggle.ScheduleDay
import io.github.vyachean.workprofiletoggle.ScheduleExactAlarmAccessState
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeActionResult
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeFailureCategory
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeProfileStatus
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeRequestedAction
import io.github.vyachean.workprofiletoggle.ScheduleRuntimeResult
import io.github.vyachean.workprofiletoggle.ScheduleTime
import io.github.vyachean.workprofiletoggle.WorkProfileSchedule
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
    private val now = ZonedDateTime.of(
        2026,
        1,
        2,
        12,
        0,
        0,
        0,
        ZoneOffset.UTC,
    )

    private val enabledSchedule = WorkProfileSchedule(
        enabled = true,
        pauseAt = ScheduleTime(hour = 18, minute = 0),
        resumeAt = ScheduleTime(hour = 9, minute = 0),
        activeDays = linkedSetOf(
            ScheduleDay.MONDAY,
            ScheduleDay.TUESDAY,
            ScheduleDay.WEDNESDAY,
            ScheduleDay.THURSDAY,
            ScheduleDay.FRIDAY,
        ),
    )

    fun activeEnabled(): HomeUiState {
        return createState()
    }

    fun setupRequired(): HomeUiState {
        return createState(
            permissionGranted = false,
            schedule = WorkProfileSchedule(),
        )
    }

    fun exactAlarmBlocked(): HomeUiState {
        return createState(
            exactAlarmAccessState = ScheduleExactAlarmAccessState.MISSING,
            runtimeResult = failedRuntimeResult(
                failureCategory = ScheduleRuntimeFailureCategory.EXACT_ALARM_ACCESS_MISSING,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
            ),
        )
    }

    fun androidRequestRejected(): HomeUiState {
        return createState(
            runtimeResult = failedRuntimeResult(
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
                actionResult = ScheduleRuntimeActionResult.FAILED,
            ),
        )
    }

    private fun createState(
        permissionGranted: Boolean = true,
        schedule: WorkProfileSchedule = enabledSchedule,
        exactAlarmAccessState: ScheduleExactAlarmAccessState = ScheduleExactAlarmAccessState.GRANTED,
        runtimeResult: ScheduleRuntimeResult? = null,
    ): HomeUiState {
        return HomeUiStateFactory.from(
            input = HomeUiStateInput(
                profilesAvailable = true,
                availableProfileCount = 1,
                selectedProfileLabel = "Work",
                selectedProfileQuietMode = false,
                permissionGranted = permissionGranted,
                schedule = schedule,
                exactAlarmAccessState = exactAlarmAccessState,
                scheduleRuntimeResult = runtimeResult,
            ),
            now = now,
        )
    }

    private fun failedRuntimeResult(
        failureCategory: ScheduleRuntimeFailureCategory,
        actionResult: ScheduleRuntimeActionResult,
    ): ScheduleRuntimeResult {
        return ScheduleRuntimeResult(
            triggerTime = now,
            expectedState = null,
            selectedProfileStatus = ScheduleRuntimeProfileStatus.NOT_CHECKED,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = actionResult,
            finalStateConfirmed = false,
            nextBoundary = null,
            failureCategory = failureCategory,
        )
    }
}
