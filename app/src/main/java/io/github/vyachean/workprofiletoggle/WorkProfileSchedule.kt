package io.github.vyachean.workprofiletoggle

internal data class WorkProfileSchedule(
    val enabled: Boolean = false,
    val pauseAt: ScheduleTime? = null,
    val resumeAt: ScheduleTime? = null,
    val activeDays: Set<ScheduleDay> = ScheduleDay.defaultSet,
)

internal data class ScheduleTime(
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }

    fun toStorageValue(): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    companion object {
        fun fromStorageValue(value: String?): ScheduleTime? {
            if (value.isNullOrBlank()) return null
            val parts = value.split(':')
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            return runCatching { ScheduleTime(hour, minute) }.getOrNull()
        }
    }
}

internal enum class ScheduleDay {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ;

    companion object {
        val defaultSet: Set<ScheduleDay> = values().toSet()

        fun fromStorageValues(values: Set<String>?): Set<ScheduleDay> {
            if (values == null) return defaultSet
            return values.mapNotNull { value ->
                runCatching { valueOf(value) }.getOrNull()
            }.toSet()
        }
    }
}
