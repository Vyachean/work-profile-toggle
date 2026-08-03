package io.github.vyachean.workprofiletoggle.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(
    name = "Active profile with enabled schedule",
    showBackground = true,
    device = "spec:width=360dp,height=800dp,dpi=420,orientation=portrait",
    apiLevel = 30,
    locale = "en",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
fun HomeScreenActiveEnabledScreenshot() {
    HomeScreenActiveEnabledScreenshotFixture()
}
