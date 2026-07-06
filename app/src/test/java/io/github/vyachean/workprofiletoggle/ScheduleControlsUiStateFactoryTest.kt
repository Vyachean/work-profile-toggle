package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleControlsUiStateFactoryTest {
    @Test
    fun exposesDefaultPickerTimesForUnconfiguredSchedule() {
        val state = ScheduleEditorUiStateFactory.from(WorkProfileSchedule())

        assertEquals(ScheduleTime(hour = 18, minute = 0), state.pauseInitialTime)
        assertEquals(ScheduleTime(hour = 9, minute = 0), state.resumeInitialTime)
        assertNull(state.enableToggle)
        assertFalse(state.showEnableRequirements)
        assertFalse(state.canClear)
    }

    @Test
    fun exposesSavedPickerTimes() {
        val state = ScheduleEditorUiStateFactory.from(
            WorkProfileSchedule(
                enabled = false,
                pauseAt = ScheduleTime(hour = 19, minute = 30),
                resumeAt = ScheduleTime(hour = 8, minute = 15),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )

        assertEquals(ScheduleTime(hour = 19, minute = 30), state.pauseInitialTime)
        assertEquals(ScheduleTime(hour = 8, minute = 15), state.resumeInitialTime)
    }

    @Test
    fun exposesEnableActionForCompleteDisabledSchedule() {
        val state = ScheduleEditorUiStateFactory.from(completeSchedule(enabled = false))

        assertEquals(ScheduleEditorEnableToggleAction.ENABLE, state.enableToggle?.action)
        assertFalse(state.showEnableRequirements)
        assertTrue(state.canClear)
    }

    @Test
    fun exposesDisableActionForCompleteEnabledSchedule() {
        val state = ScheduleEditorUiStateFactory.from(completeSchedule(enabled = true))

        assertEquals(ScheduleEditorEnableToggleAction.DISABLE, state.enableToggle?.action)
        assertFalse(state.showEnableRequirements)
        assertTrue(state.canClear)
    }

    @Test
    fun exposesRequirementsForIncompleteConfiguredSchedule() {
        val state = ScheduleEditorUiStateFactory.from(
            WorkProfileSchedule(
                enabled = false,
                pauseAt = ScheduleTime(hour = 17, minute = 0),
                resumeAt = null,
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )

        assertNull(state.enableToggle)
        assertTrue(state.showEnableRequirements)
        assertTrue(state.canClear)
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
