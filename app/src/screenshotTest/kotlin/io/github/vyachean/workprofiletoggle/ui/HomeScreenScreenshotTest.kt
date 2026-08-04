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
