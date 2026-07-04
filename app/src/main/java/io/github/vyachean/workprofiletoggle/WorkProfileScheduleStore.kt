package io.github.vyachean.workprofiletoggle

private const val PREF_SCHEDULE_ENABLED = "schedule_enabled"
private const val PREF_SCHEDULE_PAUSE_AT = "schedule_pause_at"
private const val PREF_SCHEDULE_RESUME_AT = "schedule_resume_at"
private const val PREF_SCHEDULE_ACTIVE_DAYS = "schedule_active_days"

internal class WorkProfileScheduleStore(
    private val keyValueStore: KeyValueStore,
) {
    fun load(): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = keyValueStore.getBoolean(PREF_SCHEDULE_ENABLED, false),
            pauseAt = ScheduleTime.fromStorageValue(keyValueStore.getString(PREF_SCHEDULE_PAUSE_AT)),
            resumeAt = ScheduleTime.fromStorageValue(keyValueStore.getString(PREF_SCHEDULE_RESUME_AT)),
            activeDays = ScheduleDay.fromStorageValues(
                keyValueStore.getStringSet(PREF_SCHEDULE_ACTIVE_DAYS),
            ),
        )
    }

    fun save(schedule: WorkProfileSchedule) {
        keyValueStore.edit {
            putBoolean(PREF_SCHEDULE_ENABLED, schedule.enabled)
            putString(PREF_SCHEDULE_PAUSE_AT, schedule.pauseAt?.toStorageValue())
            putString(PREF_SCHEDULE_RESUME_AT, schedule.resumeAt?.toStorageValue())
            putStringSet(
                PREF_SCHEDULE_ACTIVE_DAYS,
                schedule.activeDays.map { day -> day.name }.toSet(),
            )
        }
    }

    fun clear() {
        keyValueStore.edit {
            remove(PREF_SCHEDULE_ENABLED)
            remove(PREF_SCHEDULE_PAUSE_AT)
            remove(PREF_SCHEDULE_RESUME_AT)
            remove(PREF_SCHEDULE_ACTIVE_DAYS)
        }
    }
}
