package io.github.vyachean.workprofiletoggle.ui

internal interface HomeScreenActions {
    fun checkAgain()
    fun pauseWorkProfile()
    fun resumeWorkProfile()
    fun changeProfile()
    fun copySetupText()
    fun setPauseTime()
    fun setResumeTime()
    fun chooseActiveDays()
    fun enableSchedule()
    fun disableSchedule()
    fun openExactAlarmSettings()
    fun clearSchedule()
    fun copyDiagnostics()
    fun showAdvanced()
}

internal fun homeScreenEventHandler(actions: HomeScreenActions): HomeScreenEventHandler =
    HomeScreenEventHandler { event ->
        when (event) {
            HomeScreenEvent.CheckAgain -> actions.checkAgain()
            HomeScreenEvent.PauseWorkProfile -> actions.pauseWorkProfile()
            HomeScreenEvent.ResumeWorkProfile -> actions.resumeWorkProfile()
            HomeScreenEvent.ChangeProfile -> actions.changeProfile()
            HomeScreenEvent.CopySetupText -> actions.copySetupText()
            HomeScreenEvent.SetPauseTime -> actions.setPauseTime()
            HomeScreenEvent.SetResumeTime -> actions.setResumeTime()
            HomeScreenEvent.ChooseActiveDays -> actions.chooseActiveDays()
            HomeScreenEvent.EnableSchedule -> actions.enableSchedule()
            HomeScreenEvent.DisableSchedule -> actions.disableSchedule()
            HomeScreenEvent.OpenExactAlarmSettings -> actions.openExactAlarmSettings()
            HomeScreenEvent.ClearSchedule -> actions.clearSchedule()
            HomeScreenEvent.CopyDiagnostics -> actions.copyDiagnostics()
            HomeScreenEvent.ShowAdvanced -> actions.showAdvanced()
        }
    }
