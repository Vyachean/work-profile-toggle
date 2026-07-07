package io.github.vyachean.workprofiletoggle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.vyachean.workprofiletoggle.HomeUiState

@Composable
internal fun HomeScreenRoute(
    state: HomeUiState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
) {
    val eventHandler = remember(actions) {
        homeScreenEventHandler(actions)
    }

    HomeScreen(
        state = state,
        eventHandler = eventHandler,
        modifier = modifier,
    )
}
