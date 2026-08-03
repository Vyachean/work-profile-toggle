package io.github.vyachean.workprofiletoggle.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val WorkProfileToggleLightColors = lightColorScheme()
private val WorkProfileToggleDarkColors = darkColorScheme()

@Composable
internal fun WorkProfileToggleTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> WorkProfileToggleDarkColors
        else -> WorkProfileToggleLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
