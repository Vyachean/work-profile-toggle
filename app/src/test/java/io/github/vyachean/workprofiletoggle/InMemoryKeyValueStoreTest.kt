package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
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
        store.edit { putStringSet("days", setOf("MONDAY")) }

        val returnedSet = store.getStringSet("days")
        (returnedSet as? MutableSet<String>)?.add("TUESDAY")

        assertEquals(setOf("MONDAY"), store.getStringSet("days"))
    }
}
