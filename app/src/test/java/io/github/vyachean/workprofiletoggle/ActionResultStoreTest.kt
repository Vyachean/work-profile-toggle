package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionResultStoreTest {
    @Test
    fun returnsSavedInstanceStateBeforePersistedResult() {
        val store = ActionResultStore(
            keyValueStore = InMemoryKeyValueStore(),
            defaultResult = "Default result",
        )
        store.save("Persisted result")

        assertEquals("Saved result", store.restore("Saved result"))
    }

    @Test
    fun returnsPersistedResultWhenSavedInstanceStateIsMissing() {
        val store = ActionResultStore(
            keyValueStore = InMemoryKeyValueStore(),
            defaultResult = "Default result",
        )
        store.save("Persisted result")

        assertEquals("Persisted result", store.restore(null))
    }

    @Test
    fun returnsDefaultResultWhenNoSavedOrPersistedResultExists() {
        val store = ActionResultStore(
            keyValueStore = InMemoryKeyValueStore(),
            defaultResult = "Default result",
        )

        assertEquals("Default result", store.restore(null))
    }

    @Test
    fun savesAndRestoresResult() {
        val store = ActionResultStore(
            keyValueStore = InMemoryKeyValueStore(),
            defaultResult = "Default result",
        )

        store.save("Action succeeded")

        assertEquals("Action succeeded", store.restore(null))
    }
}
