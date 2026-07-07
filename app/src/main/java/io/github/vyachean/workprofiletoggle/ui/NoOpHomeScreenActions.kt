package io.github.vyachean.workprofiletoggle.ui

internal object NoOpHomeScreenActions : HomeScreenActions {
    override fun checkAgain() = Unit
    override fun pauseWorkProfile() = Unit
    override fun resumeWorkProfile() = Unit
    override fun changeProfile() = Unit
    override fun setPauseTime() = Unit
    override fun setResumeTime() = Unit
    override fun chooseActiveDays() = Unit
    override fun enableSchedule() = Unit
    override fun disableSchedule() = Unit
    override fun clearSchedule() = Unit
    override fun copyDiagnostics() = Unit
}
