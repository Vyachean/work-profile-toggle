package io.github.vyachean.workprofiletoggle

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun loadsDefaultScheduleWhenPreferencesAreEmpty() {
        val store = WorkProfileScheduleStore(FakeSharedPreferences())

        assertEquals(WorkProfileSchedule(), store.load())
    }

    @Test
    fun savesAndLoadsSchedule() {
        val store = WorkProfileScheduleStore(FakeSharedPreferences())
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
        val store = WorkProfileScheduleStore(FakeSharedPreferences())
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
        val preferences = FakeSharedPreferences()
        preferences.edit()
            .putBoolean("schedule_enabled", true)
            .putString("schedule_pause_at", "24:00")
            .putString("schedule_resume_at", "09:99")
            .putStringSet("schedule_active_days", setOf("MONDAY", "NOT_A_DAY"))
            .apply()
        val store = WorkProfileScheduleStore(preferences)

        val schedule = store.load()

        assertTrue(schedule.enabled)
        assertNull(schedule.pauseAt)
        assertNull(schedule.resumeAt)
        assertEquals(setOf(ScheduleDay.MONDAY), schedule.activeDays)
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> {
        return values.toMutableMap()
    }

    override fun getString(key: String, defValue: String?): String? {
        return values[key] as? String ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
        return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return values[key] as? Int ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return values[key] as? Long ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return values[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defValue
    }

    override fun contains(key: String): Boolean {
        return values.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return FakeEditor()
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners.remove(listener)
    }

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pendingValues = mutableMapOf<String, Any?>()
        private val removedKeys = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pendingValues[key] = value
            removedKeys.remove(key)
            return this
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
            pendingValues[key] = values?.toSet()
            removedKeys.remove(key)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pendingValues[key] = value
            removedKeys.remove(key)
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pendingValues[key] = value
            removedKeys.remove(key)
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pendingValues[key] = value
            removedKeys.remove(key)
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pendingValues[key] = value
            removedKeys.remove(key)
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removedKeys.add(key)
            pendingValues.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            pendingValues.clear()
            removedKeys.clear()
            return this
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            if (clearRequested) {
                values.clear()
            }
            removedKeys.forEach { key -> values.remove(key) }
            pendingValues.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
            (removedKeys + pendingValues.keys).forEach { changedKey ->
                listeners.forEach { listener ->
                    listener.onSharedPreferenceChanged(this@FakeSharedPreferences, changedKey)
                }
            }
        }
    }
}
