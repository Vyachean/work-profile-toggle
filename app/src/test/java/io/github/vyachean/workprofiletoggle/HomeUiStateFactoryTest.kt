package io.github.vyachean.workprofiletoggle

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateFactoryTest {
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 10, 30, 0, 0, ZoneId.of("UTC"))

    @Test
    fun showsNoWorkProfileWhenProfilesAreUnavailable() {
        val state = HomeUiStateFactory.from(
            input = input(
                profilesAvailable = false,
                availableProfileCount = 0,
            ),
            now = now,
        )

        assertEquals(HomePrimaryState.NO_WORK_PROFILE, state.primary)
        assertFalse(state.setup.ready)
        assertFalse(state.setup.profileFound)
    }

    @Test
    fun asksUserToChooseProfileWhenNoProfileIsSelected() {
        val state = HomeUiStateFactory.from(
            input = input(
                availableProfileCount = 2,
                selectedProfileLabel = null,
                permissionGranted = true,
            ),
            now = now,
        )

        assertEquals(HomePrimaryState.CHOOSE_WORK_PROFILE, state.primary)
        assertFalse(state.setup.ready)
        assertTrue(state.setup.profileFound)
        assertNull(state.setup.selectedProfileLabel)
    }

    @Test
    fun showsSetupRequiredBeforePermissionIsGranted() {
        val state = HomeUiStateFactory.from(
            input = input(
                selectedProfileLabel = "Work",
                permissionGranted = false,
                selectedProfileQuietMode = false,
            ),
            now = now,
        )

        assertEquals(HomePrimaryState.SETUP_REQUIRED, state.primary)
        assertFalse(state.setup.ready)
        assertEquals("Work", state.setup.selectedProfileLabel)
    }

    @Test
    fun showsActiveProfileWhenSetupIsReadyAndQuietModeIsDisabled() {
        val state = HomeUiStateFactory.from(
            input = input(
                selectedProfileLabel = "Work",
                permissionGranted = true,
                selectedProfileQuietMode = false,
            ),
            now = now,
        )

        assertEquals(HomePrimaryState.WORK_PROFILE_ACTIVE, state.primary)
        assertTrue(state.setup.ready)
    }

    @Test
    fun showsPausedProfileWhenSetupIsReadyAndQuietModeIsEnabled() {
        val state = HomeUiStateFactory.from(
            input = input(
                selectedProfileLabel = "Work",
                permissionGranted = true,
                selectedProfileQuietMode = true,
            ),
            now = now,
        )

        assertEquals(HomePrimaryState.WORK_PROFILE_PAUSED, state.primary)
        assertTrue(state.setup.ready)
    }

    @Test
    fun marksMissingExactAlarmAccessAsBlockedScheduleState() {
        val state = HomeUiStateFactory.from(
            input = input(
                schedule = completeSchedule(enabled = true),
                exactAlarmAccessState = ScheduleExactAlarmAccessState.MISSING,
            ),
            now = now,
        )

        assertTrue(state.schedule.configured)
        assertEquals(HomeScheduleSavedState.BLOCKED_EXACT_ALARM_ACCESS, state.schedule.savedState)
        assertEquals(ScheduleExactAlarmAccessState.MISSING, state.schedule.exactAlarmAccessState)
        assertTrue(state.schedule.canCopyDiagnostics)
    }

    @Test
    fun exposesIncompleteScheduleControlsState() {
        val state = HomeUiStateFactory.from(
            input = input(
                schedule = WorkProfileSchedule(
                    enabled = false,
                    pauseAt = ScheduleTime(hour = 17, minute = 0),
                    resumeAt = null,
                    activeDays = setOf(ScheduleDay.MONDAY),
                ),
            ),
            now = now,
        )

        assertTrue(state.schedule.configured)
        assertEquals(HomeScheduleSavedState.DISABLED, state.schedule.savedState)
        assertFalse(state.schedule.enableToggleAvailable)
        assertTrue(state.schedule.showEnableRequirements)
    }

    @Test
    fun exposesScheduleRuntimeNextAction() {
        val state = HomeUiStateFactory.from(
            input = input(
                schedule = completeSchedule(enabled = true),
            ),
            now = now,
        )

        assertEquals(HomeScheduleSavedState.ENABLED, state.schedule.savedState)
        assertEquals(
            ScheduleRuntimeNextActionType.PAUSE_WORK_PROFILE,
            state.schedule.runtimeStatus?.nextAction?.type,
        )
        assertNull(state.schedule.runtimeStatus?.issue)
    }

    private fun input(
        profilesAvailable: Boolean = true,
        availableProfileCount: Int = 1,
        selectedProfileLabel: String? = "Work",
        selectedProfileQuietMode: Boolean? = false,
        permissionGranted: Boolean = true,
        schedule: WorkProfileSchedule = WorkProfileSchedule(),
        exactAlarmAccessState: ScheduleExactAlarmAccessState = ScheduleExactAlarmAccessState.GRANTED,
        scheduleRuntimeResult: ScheduleRuntimeResult? = null,
    ): HomeUiStateInput {
        return HomeUiStateInput(
            profilesAvailable = profilesAvailable,
            availableProfileCount = availableProfileCount,
            selectedProfileLabel = selectedProfileLabel,
            selectedProfileQuietMode = selectedProfileQuietMode,
            permissionGranted = permissionGranted,
            schedule = schedule,
            exactAlarmAccessState = exactAlarmAccessState,
            scheduleRuntimeResult = scheduleRuntimeResult,
        )
    }

    private fun completeSchedule(enabled: Boolean): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = enabled,
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            pauseAt = ScheduleTime(hour = 17, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY),
        )
    }
}
