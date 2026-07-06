package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WorkProfileToggleLightColors = lightColorScheme()

@Composable
internal fun WorkProfileToggleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkProfileToggleLightColors,
        content = content,
    )
}
