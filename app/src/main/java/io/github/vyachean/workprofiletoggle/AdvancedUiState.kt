package io.github.vyachean.workprofiletoggle

internal data class AdvancedUiState(
    val lastResult: String,
    val profilesFound: Int,
    val shortcutSummary: AdvancedShortcutUiState?,
    val showNoSwitchableProfiles: Boolean,
)

internal data class AdvancedShortcutUiState(
    val shortcutsCount: Int,
    val maxShortcuts: Int,
)

internal object AdvancedUiStateFactory {
    fun from(
        lastResult: String,
        profilesFound: Int,
        shortcutUpdateState: ShortcutUpdateState?,
    ): AdvancedUiState {
        return AdvancedUiState(
            lastResult = lastResult,
            profilesFound = profilesFound,
            shortcutSummary = shortcutUpdateState?.let { updateState ->
                AdvancedShortcutUiState(
                    shortcutsCount = updateState.shortcutsCount,
                    maxShortcuts = updateState.maxShortcuts,
                )
            },
            showNoSwitchableProfiles = profilesFound == 0,
        )
    }
}
