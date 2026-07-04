package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTimeTest {
    @Test
    fun parsesValidStorageValue() {
        assertEquals(ScheduleTime(hour = 9, minute = 30), ScheduleTime.fromStorageValue("09:30"))
    }

    @Test
    fun returnsNullForInvalidStorageValue() {
        val invalidValues = listOf(
            null,
            "",
            "09",
            "09-30",
            "24:00",
            "10:60",
            "aa:30",
            "10:bb",
        )

        invalidValues.forEach { value ->
            assertNull(ScheduleTime.fromStorageValue(value))
        }
    }

    @Test
    fun formatsStorageValueWithPaddedHourAndMinute() {
        assertEquals("09:05", ScheduleTime(hour = 9, minute = 5).toStorageValue())
    }
}

class ScheduleDayTest {
    @Test
    fun returnsDefaultDaysWhenStorageValueIsMissing() {
        assertEquals(ScheduleDay.defaultSet, ScheduleDay.fromStorageValues(null))
    }

    @Test
    fun parsesKnownStorageValues() {
        assertEquals(
            setOf(ScheduleDay.MONDAY, ScheduleDay.FRIDAY),
            ScheduleDay.fromStorageValues(setOf("MONDAY", "FRIDAY")),
        )
    }

    @Test
    fun ignoresUnknownStorageValues() {
        assertEquals(
            setOf(ScheduleDay.MONDAY),
            ScheduleDay.fromStorageValues(setOf("MONDAY", "NOT_A_DAY")),
        )
    }
}

class WorkProfileScheduleStoreTest {
    @Test
    fun loadsDefaultScheduleWhenStoreIsEmpty() {
        val store = WorkProfileScheduleStore(FakeKeyValueStore())

        assertEquals(WorkProfileSchedule(), store.load())
    }

    @Test
    fun savesAndLoadsSchedule() {
        val store = WorkProfileScheduleStore(FakeKeyValueStore())
        val schedule = WorkProfileSchedule(
            enabled = true,
            pauseAt = ScheduleTime(hour = 18, minute = 15),
            resumeAt = ScheduleTime(hour = 9, minute = 0),
            activeDays = setOf(ScheduleDay.MONDAY, ScheduleDay.TUESDAY),
        )

        store.save(schedule)

        assertEquals(schedule, store.load())
    }

    @Test
    fun clearsSavedSchedule() {
        val store = WorkProfileScheduleStore(FakeKeyValueStore())
        store.save(
            WorkProfileSchedule(
                enabled = true,
                pauseAt = ScheduleTime(hour = 18, minute = 0),
                resumeAt = ScheduleTime(hour = 9, minute = 0),
                activeDays = setOf(ScheduleDay.MONDAY),
            ),
        )

        store.clear()

        assertEquals(WorkProfileSchedule(), store.load())
    }

    @Test
    fun ignoresCorruptedPersistedValues() {
        val keyValueStore = FakeKeyValueStore()
        keyValueStore.edit {
            putBoolean("schedule_enabled", true)
            putString("schedule_pause_at", "24:00")
            putString("schedule_resume_at", "09:99")
            putStringSet("schedule_active_days", setOf("MONDAY", "NOT_A_DAY"))
        }
        val store = WorkProfileScheduleStore(keyValueStore)

        val schedule = store.load()

        assertTrue(schedule.enabled)
        assertNull(schedule.pauseAt)
        assertNull(schedule.resumeAt)
        assertEquals(setOf(ScheduleDay.MONDAY), schedule.activeDays)
    }
}

private class FakeKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any?>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defaultValue
    }

    override fun getString(key: String): String? {
        return values[key] as? String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String): Set<String>? {
        return values[key] as? Set<String>
    }

    override fun edit(update: KeyValueStoreEditor.() -> Unit) {
        FakeKeyValueStoreEditor().apply {
            update()
            applyChanges()
        }
    }

    private inner class FakeKeyValueStoreEditor : KeyValueStoreEditor {
        private val pendingValues = mutableMapOf<String, Any?>()
        private val removedKeys = mutableSetOf<String>()

        override fun putBoolean(key: String, value: Boolean) {
            pendingValues[key] = value
            removedKeys.remove(key)
        }

        override fun putString(key: String, value: String?) {
            pendingValues[key] = value
            removedKeys.remove(key)
        }

        override fun putStringSet(key: String, values: Set<String>) {
            pendingValues[key] = values
            removedKeys.remove(key)
        }

        override fun remove(key: String) {
            removedKeys.add(key)
            pendingValues.remove(key)
        }

        fun applyChanges() {
            removedKeys.forEach { key -> values.remove(key) }
            pendingValues.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
        }
    }
}
