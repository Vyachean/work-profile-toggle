package io.github.vyachean.workprofiletoggle.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenActionsTest {
    @Test
    fun eventHandlerDispatchesEveryHomeAction() {
        val actions = RecordingHomeScreenActions()
        val handler = homeScreenEventHandler(actions)
        val cases = listOf(
            HomeScreenEvent.CheckAgain to "checkAgain",
            HomeScreenEvent.PauseWorkProfile to "pauseWorkProfile",
            HomeScreenEvent.ResumeWorkProfile to "resumeWorkProfile",
            HomeScreenEvent.ChangeProfile to "changeProfile",
            HomeScreenEvent.CopySetupText to "copySetupText",
            HomeScreenEvent.SetPauseTime to "setPauseTime",
            HomeScreenEvent.SetResumeTime to "setResumeTime",
            HomeScreenEvent.ChooseActiveDays to "chooseActiveDays",
            HomeScreenEvent.EnableSchedule to "enableSchedule",
            HomeScreenEvent.DisableSchedule to "disableSchedule",
            HomeScreenEvent.OpenExactAlarmSettings to "openExactAlarmSettings",
            HomeScreenEvent.ClearSchedule to "clearSchedule",
            HomeScreenEvent.CopyDiagnostics to "copyDiagnostics",
            HomeScreenEvent.ShowAdvanced to "showAdvanced",
        )

        cases.forEach { (event, expectedAction) ->
            actions.lastAction = null
            handler.onHomeScreenEvent(event)
            assertEquals(expectedAction, actions.lastAction)
        }
    }
}

private class RecordingHomeScreenActions : HomeScreenActions {
    var lastAction: String? = null

    override fun checkAgain() = record("checkAgain")
    override fun pauseWorkProfile() = record("pauseWorkProfile")
    override fun resumeWorkProfile() = record("resumeWorkProfile")
    override fun changeProfile() = record("changeProfile")
    override fun copySetupText() = record("copySetupText")
    override fun setPauseTime() = record("setPauseTime")
    override fun setResumeTime() = record("setResumeTime")
    override fun chooseActiveDays() = record("chooseActiveDays")
    override fun enableSchedule() = record("enableSchedule")
    override fun disableSchedule() = record("disableSchedule")
    override fun openExactAlarmSettings() = record("openExactAlarmSettings")
    override fun clearSchedule() = record("clearSchedule")
    override fun copyDiagnostics() = record("copyDiagnostics")
    override fun showAdvanced() = record("showAdvanced")

    private fun record(action: String) {
        lastAction = action
    }
}
