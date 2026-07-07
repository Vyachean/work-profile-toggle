package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleSavePolicyTest {
    @Test
    fun treatsScheduleAsCompleteOnlyWhenPauseResumeAndDaysAreSet() {
        assertFalse(ScheduleSavePolicy.isComplete(WorkProfileSchedule()))
        assertFalse(
            ScheduleSavePolicy.isComplete(
                completeSchedule().copy(pauseAt = null),
            ),
        )
        assertFalse(
            ScheduleSavePolicy.isComplete(
                completeSchedule().copy(resumeAt = null),
            ),
        )
        assertFalse(
            ScheduleSavePolicy.isComplete(
                completeSchedule().copy(activeDays = emptySet()),
            ),
        )
        assertTrue(ScheduleSavePolicy.isComplete(completeSchedule()))
    }

    @Test
    fun keepsCompleteScheduleUnchanged() {
        val schedule = completeSchedule(enabled = true)

        assertEquals(schedule, ScheduleSavePolicy.normalizeForSave(schedule))
    }

    @Test
    fun disablesIncompleteScheduleBeforeSaving() {
        val incompleteSchedule = completeSchedule(enabled = true).copy(activeDays = emptySet())

        assertEquals(
            incompleteSchedule.copy(enabled = false),
            ScheduleSavePolicy.normalizeForSave(incompleteSchedule),
        )
    }

    private fun completeSchedule(enabled: Boolean = false): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = enabled,
            pauseAt = ScheduleTime(hour = 18, minute = 0),
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY),
        )
    }
}
