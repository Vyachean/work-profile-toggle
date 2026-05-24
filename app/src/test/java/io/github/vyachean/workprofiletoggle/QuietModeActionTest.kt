package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuietModeActionTest {
    @Test
    fun fromIntentActionReturnsEnableAction() {
        assertEquals(
            QuietModeAction.Enable,
            QuietModeAction.fromIntentAction("io.github.vyachean.workprofiletoggle.action.ENABLE_QUIET_MODE"),
        )
    }

    @Test
    fun fromIntentActionReturnsDisableAction() {
        assertEquals(
            QuietModeAction.Disable,
            QuietModeAction.fromIntentAction("io.github.vyachean.workprofiletoggle.action.DISABLE_QUIET_MODE"),
        )
    }

    @Test
    fun fromIntentActionReturnsToggleAction() {
        assertEquals(
            QuietModeAction.Toggle,
            QuietModeAction.fromIntentAction("io.github.vyachean.workprofiletoggle.action.TOGGLE_QUIET_MODE"),
        )
    }

    @Test
    fun fromIntentActionReturnsNullForUnknownAction() {
        assertNull(QuietModeAction.fromIntentAction("unknown"))
    }

    @Test
    fun fromIntentActionReturnsNullForMissingAction() {
        assertNull(QuietModeAction.fromIntentAction(null))
    }
}
