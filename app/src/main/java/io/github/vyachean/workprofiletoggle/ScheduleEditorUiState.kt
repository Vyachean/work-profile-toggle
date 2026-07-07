package io.github.vyachean.workprofiletoggle

internal data class ScheduleEditorUiState(
    val pauseInitialTime: ScheduleTime,
    val resumeInitialTime: ScheduleTime,
    val enableToggle: ScheduleEditorEnableToggle?,
    val showEnableRequirements: Boolean,
    val canClear: Boolean,
)

internal data class ScheduleEditorEnableToggle(
    val action: ScheduleEditorEnableToggleAction,
)

internal enum class ScheduleEditorEnableToggleAction {
    ENABLE,
    DISABLE,
}

internal object ScheduleEditorUiStateFactory {
    private val defaultPauseTime = ScheduleTime(hour = 18, minute = 0)
    private val defaultResumeTime = ScheduleTime(hour = 9, minute = 0)

    fun from(schedule: WorkProfileSchedule): ScheduleEditorUiState {
        val configured = schedule != WorkProfileSchedule()
        val complete = ScheduleSavePolicy.isComplete(schedule)
        return ScheduleEditorUiState(
            pauseInitialTime = schedule.pauseAt ?: defaultPauseTime,
            resumeInitialTime = schedule.resumeAt ?: defaultResumeTime,
            enableToggle = if (configured && complete) {
                ScheduleEditorEnableToggle(
                    action = if (schedule.enabled) {
                        ScheduleEditorEnableToggleAction.DISABLE
                    } else {
                        ScheduleEditorEnableToggleAction.ENABLE
                    },
                )
            } else {
                null
            },
            showEnableRequirements = configured && !complete,
            canClear = configured,
        )
    }
}
