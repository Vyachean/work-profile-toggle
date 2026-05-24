package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileLabelsTest {
    @Test
    fun fromSerialNumbersSortsProfilesBySerialNumber() {
        val profiles = ProfileLabels.fromSerialNumbers(listOf(12, 0, 11))

        assertEquals(
            listOf(0L, 11L, 12L),
            profiles.map { it.identifier.serialNumber },
        )
    }

    @Test
    fun fromSerialNumbersCreatesDeterministicFallbackLabels() {
        val profiles = ProfileLabels.fromSerialNumbers(listOf(12, 0, 11))

        assertEquals(
            listOf(
                "Profile 1 (serial 0)",
                "Profile 2 (serial 11)",
                "Profile 3 (serial 12)",
            ),
            profiles.map { it.label },
        )
    }

    @Test
    fun fromSerialNumbersRemovesDuplicateSerialNumbers() {
        val profiles = ProfileLabels.fromSerialNumbers(listOf(12, 12, 0, 0, 11))

        assertEquals(
            listOf(0L, 11L, 12L),
            profiles.map { it.identifier.serialNumber },
        )
    }
}
