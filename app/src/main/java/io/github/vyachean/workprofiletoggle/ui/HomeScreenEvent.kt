package io.github.vyachean.workprofiletoggle.ui

internal sealed interface HomeScreenEvent {
    data object CheckAgain : HomeScreenEvent
    data object PauseWorkProfile : HomeScreenEvent
    data object ResumeWorkProfile : HomeScreenEvent
    data object ChangeProfile : HomeScreenEvent
    data object SetPauseTime : HomeScreenEvent
    data object SetResumeTime : HomeScreenEvent
    data object ChooseActiveDays : HomeScreenEvent
    data object EnableSchedule : HomeScreenEvent
    data object DisableSchedule : HomeScreenEvent
    data object ClearSchedule : HomeScreenEvent
    data object CopyDiagnostics : HomeScreenEvent
}
