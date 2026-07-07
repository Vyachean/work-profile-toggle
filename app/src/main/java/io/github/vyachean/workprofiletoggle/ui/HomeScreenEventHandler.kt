package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Stable

@Stable
internal fun interface HomeScreenEventHandler {
    fun onHomeScreenEvent(event: HomeScreenEvent)
}
