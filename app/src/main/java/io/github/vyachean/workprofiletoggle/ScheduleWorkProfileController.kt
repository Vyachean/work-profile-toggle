package io.github.vyachean.workprofiletoggle

internal interface ScheduleWorkProfileHandle

internal sealed class ScheduleWorkProfileResolution {
    data class Selected(
        val handle: ScheduleWorkProfileHandle,
    ) : ScheduleWorkProfileResolution()

    object Missing : ScheduleWorkProfileResolution()
    object Unavailable : ScheduleWorkProfileResolution()
}

internal sealed class ScheduleWorkProfileDispatchResult {
    object Completed : ScheduleWorkProfileDispatchResult()
    object Ignored : ScheduleWorkProfileDispatchResult()
    object MissingProfile : ScheduleWorkProfileDispatchResult()

    data class Failed(
        val error: Throwable,
    ) : ScheduleWorkProfileDispatchResult()
}

internal interface ScheduleWorkProfileController {
    fun resolveSelectedProfile(): ScheduleWorkProfileResolution
    fun isQuietModeEnabled(handle: ScheduleWorkProfileHandle): Result<Boolean>
    fun dispatch(handle: ScheduleWorkProfileHandle, requestedAction: QuietModeAction): ScheduleWorkProfileDispatchResult
}
