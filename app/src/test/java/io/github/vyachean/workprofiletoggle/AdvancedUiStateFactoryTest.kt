package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedUiStateFactoryTest {
    @Test
    fun exposesLastResultAndProfileCount() {
        val state = AdvancedUiStateFactory.from(
            lastResult = "No action executed yet.",
            profilesFound = 2,
            shortcutUpdateState = null,
        )

        assertEquals("No action executed yet.", state.lastResult)
        assertEquals(2, state.profilesFound)
        assertNull(state.shortcutSummary)
        assertFalse(state.showNoSwitchableProfiles)
    }

    @Test
    fun exposesNoSwitchableProfilesState() {
        val state = AdvancedUiStateFactory.from(
            lastResult = "No action executed yet.",
            profilesFound = 0,
            shortcutUpdateState = null,
        )

        assertTrue(state.showNoSwitchableProfiles)
    }

    @Test
    fun exposesShortcutSummary() {
        val state = AdvancedUiStateFactory.from(
            lastResult = "Done.",
            profilesFound = 1,
            shortcutUpdateState = ShortcutUpdateState(
                shortcutsCount = 3,
                maxShortcuts = 15,
            ),
        )

        assertEquals(3, state.shortcutSummary?.shortcutsCount)
        assertEquals(15, state.shortcutSummary?.maxShortcuts)
    }
}
