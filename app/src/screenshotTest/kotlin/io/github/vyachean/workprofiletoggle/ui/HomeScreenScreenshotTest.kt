package io.github.vyachean.workprofiletoggle.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@Preview(
    name = "Phone light",
    showBackground = true,
    device = "spec:width=360dp,height=800dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
private annotation class PhoneLightScreenshot

@Preview(
    name = "Phone light",
    showBackground = true,
    device = "spec:width=360dp,height=800dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Phone dark",
    showBackground = true,
    device = "spec:width=360dp,height=800dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Compact light",
    showBackground = true,
    device = "spec:width=320dp,height=480dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Large font light",
    showBackground = true,
    device = "spec:width=360dp,height=800dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.5f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
private annotation class HomeScreenshotConfigurations

@PreviewTest
@HomeScreenshotConfigurations
@Composable
fun HomeScreenActiveEnabledScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.activeEnabled(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenNoWorkProfileScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.noWorkProfile(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenChooseWorkProfileScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.chooseWorkProfile(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenPausedEnabledScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.pausedEnabled(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenProfileStateUnknownScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.profileStateUnknown(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenIncompleteScheduleScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.incompleteSchedule(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenDisabledScheduleScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.disabledSchedule(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenSetupRequiredScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.setupRequired(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenExactAlarmBlockedScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.exactAlarmBlocked(),
    )
}

@PreviewTest
@PhoneLightScreenshot
@Composable
fun HomeScreenAndroidRequestRejectedScreenshot() {
    HomeScreenScreenshotFixture(
        state = HomeScreenScreenshotStates.androidRequestRejected(),
    )
}
