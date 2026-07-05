package io.github.vyachean.workprofiletoggle

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRescheduleReceiverTest {
    @Test
    fun returnsTrueForSupportedRescheduleActions() {
        assertTrue(isScheduleRescheduleAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(isScheduleRescheduleAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(isScheduleRescheduleAction(Intent.ACTION_TIME_CHANGED))
        assertTrue(isScheduleRescheduleAction(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun returnsFalseForUnknownOrMissingAction() {
        assertFalse(isScheduleRescheduleAction(null))
        assertFalse(isScheduleRescheduleAction(ACTION_SCHEDULE_BOUNDARY))
        assertFalse(isScheduleRescheduleAction("unknown"))
    }
}
