package io.github.vyachean.workprofiletoggle

internal object ScheduleSavePolicy {
    fun normalizeForSave(schedule: WorkProfileSchedule): WorkProfileSchedule {
        return if (isComplete(schedule)) {
            schedule
        } else {
            schedule.copy(enabled = false)
        }
    }

    fun isComplete(schedule: WorkProfileSchedule): Boolean {
        return schedule.pauseAt != null &&
            schedule.resumeAt != null &&
            schedule.activeDays.isNotEmpty()
    }
}
