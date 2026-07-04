package io.github.vyachean.workprofiletoggle

import android.content.SharedPreferences

private const val PREF_SCHEDULE_ENABLED = "schedule_enabled"
private const val PREF_SCHEDULE_PAUSE_AT = "schedule_pause_at"
private const val PREF_SCHEDULE_RESUME_AT = "schedule_resume_at"
private const val PREF_SCHEDULE_ACTIVE_DAYS = "schedule_active_days"

internal class WorkProfileScheduleStore(
    private val preferences: SharedPreferences,
) {
    fun load(): WorkProfileSchedule {
        return WorkProfileSchedule(
            enabled = preferences.getBoolean(PREF_SCHEDULE_ENABLED, false),
            pauseAt = ScheduleTime.fromStorageValue(preferences.getString(PREF_SCHEDULE_PAUSE_AT, null)),
            resumeAt = ScheduleTime.fromStorageValue(preferences.getString(PREF_SCHEDULE_RESUME_AT, null)),
            activeDays = ScheduleDay.fromStorageValues(
                preferences.getStringSet(PREF_SCHEDULE_ACTIVE_DAYS, null),
            ),
        )
    }

    fun save(schedule: WorkProfileSchedule) {
        preferences.edit()
            .putBoolean(PREF_SCHEDULE_ENABLED, schedule.enabled)
            .putString(PREF_SCHEDULE_PAUSE_AT, schedule.pauseAt?.toStorageValue())
            .putString(PREF_SCHEDULE_RESUME_AT, schedule.resumeAt?.toStorageValue())
            .putStringSet(
                PREF_SCHEDULE_ACTIVE_DAYS,
                schedule.activeDays.map { day -> day.name }.toSet(),
            )
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(PREF_SCHEDULE_ENABLED)
            .remove(PREF_SCHEDULE_PAUSE_AT)
            .remove(PREF_SCHEDULE_RESUME_AT)
            .remove(PREF_SCHEDULE_ACTIVE_DAYS)
            .apply()
    }
}
