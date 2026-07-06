package io.github.vyachean.workprofiletoggle

import java.time.ZonedDateTime

internal object ScheduleRuntimeDiagnosticsFormatter {
    fun format(
        appVersionName: String,
        currentTime: ZonedDateTime,
        schedule: WorkProfileSchedule,
        exactAlarmAccessState: ScheduleExactAlarmAccessState,
        runtimeResult: ScheduleRuntimeResult?,
    ): String {
        val runtime = runtimeResult
        return buildString {
            appendLine("Work Profile Toggle schedule diagnostics")
            appendLine("app.versionName=" + appVersionName.ifBlank { "unknown" })
            appendLine("current.time=" + currentTime)
            appendLine("current.timezone=" + currentTime.zone.id)
            appendLine("schedule.enabled=" + schedule.enabled)
            appendLine("schedule.activeDays=" + schedule.activeDays.formatDays())
            appendLine("schedule.resumeAt=" + schedule.resumeAt.formatTime())
            appendLine("schedule.pauseAt=" + schedule.pauseAt.formatTime())
            appendLine("exactAlarmAccess=" + exactAlarmAccessState)
            appendLine("runtimeResult.present=" + (runtime != null))
            appendLine("runtime.triggerTime=" + runtime?.triggerTime.formatNullable())
            appendLine("runtime.expectedState=" + runtime?.expectedState.formatNullable())
            appendLine("runtime.selectedProfileStatus=" + runtime?.selectedProfileStatus.formatNullable())
            appendLine("runtime.requestedAction=" + runtime?.requestedAction.formatNullable())
            appendLine("runtime.actionResult=" + runtime?.actionResult.formatNullable())
            appendLine("runtime.finalStateConfirmed=" + runtime?.finalStateConfirmed.formatNullable())
            appendLine("runtime.nextBoundary.at=" + runtime?.nextBoundary?.at.formatNullable())
            appendLine("runtime.nextBoundary.expectedState=" + runtime?.nextBoundary?.expectedState.formatNullable())
            appendLine("runtime.failureCategory=" + runtime?.failureCategory.formatNullable())
        }.trimEnd()
    }

    private fun Set<ScheduleDay>.formatDays(): String {
        return if (isEmpty()) {
            "none"
        } else {
            sorted().joinToString(",") { day -> day.name }
        }
    }

    private fun ScheduleTime?.formatTime(): String {
        return this?.toStorageValue() ?: "null"
    }

    private fun Any?.formatNullable(): String {
        return this?.toString() ?: "null"
    }
}
