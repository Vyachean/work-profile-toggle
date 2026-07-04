package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class InMemoryKeyValueStoreTest {
    @Test
    fun copiesStringSetWhenSaving() {
        val sourceSet = mutableSetOf("MONDAY")
        val store = InMemoryKeyValueStore()

        store.edit { putStringSet("days", sourceSet) }
        sourceSet.add("TUESDAY")

        assertEquals(setOf("MONDAY"), store.getStringSet("days"))
    }

    @Test
    fun copiesStringSetWhenReading() {
        val store = InMemoryKeyValueStore()
        store.edit { putStringSet("days", setOf("MONDAY", "TUESDAY")) }

        val firstRead = store.getStringSet("days")
        val secondRead = store.getStringSet("days")

        assertEquals(setOf("MONDAY", "TUESDAY"), firstRead)
        assertEquals(setOf("MONDAY", "TUESDAY"), secondRead)
        assertNotSame(firstRead, secondRead)
    }
}
