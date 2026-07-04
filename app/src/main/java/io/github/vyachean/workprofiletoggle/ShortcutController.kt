package io.github.vyachean.workprofiletoggle

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import java.util.Locale

internal class ShortcutController(
    private val context: Context,
    private val shortcutManager: ShortcutManager?,
) {
    private var lastShortcutSignature: List<ShortcutDescriptor>? = null

    fun updateShortcuts(profiles: List<ProfileEntry.Labeled>): Result<ShortcutUpdateState?> {
        return runCatching {
            val manager = shortcutManager ?: return@runCatching null
            val maxShortcuts = manager.maxShortcutCountPerActivity
            val shortcutProfileCount = maxShortcuts / QuietModeAction.entries.size
            val shortcutDescriptors = profiles
                .take(shortcutProfileCount)
                .flatMapIndexed { index, profileEntry ->
                    quietModeShortcutDescriptors(profileEntry, shortcutProfileLabel(index))
                }

            if (shortcutDescriptors != lastShortcutSignature) {
                manager.dynamicShortcuts = shortcutDescriptors.map { descriptor ->
                    ShortcutInfo.Builder(context, descriptor.id)
                        .setShortLabel(descriptor.shortLabel)
                        .setLongLabel(descriptor.longLabel)
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_quiet_mode))
                        .setIntent(shortcutIntent(descriptor.action, descriptor.serialNumber))
                        .build()
                }
                lastShortcutSignature = shortcutDescriptors
            }

            ShortcutUpdateState(
                shortcutsCount = shortcutDescriptors.size,
                maxShortcuts = maxShortcuts,
            )
        }
    }

    private fun quietModeShortcutDescriptors(
        profileEntry: ProfileEntry.Labeled,
        shortcutProfileLabel: String,
    ): List<ShortcutDescriptor> {
        return QuietModeAction.entries.map { action ->
            ShortcutDescriptor(
                id = shortcutId(action, profileEntry.profile.identifier.serialNumber),
                action = action,
                serialNumber = profileEntry.profile.identifier.serialNumber,
                shortLabel = shortcutShortLabel(action, shortcutProfileLabel),
                longLabel = shortcutLongLabel(action, profileEntry.profile.label),
            )
        }
    }

    private fun shortcutProfileLabel(index: Int): String {
        return context.getString(R.string.shortcut_profile_short_label, index + 1)
    }

    private fun shortcutId(action: QuietModeAction, serialNumber: Long): String {
        return "${action.name.lowercase(Locale.ROOT)}-$serialNumber"
    }

    private fun shortcutIntent(action: QuietModeAction, serialNumber: Long): Intent {
        return Intent(context, QuietModeActionActivity::class.java).apply {
            this.action = action.intentAction
            putExtra(EXTRA_PROFILE_SERIAL, serialNumber)
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
    }

    private fun shortcutShortLabel(action: QuietModeAction, profileLabel: String): String {
        val stringId = when (action) {
            QuietModeAction.Enable -> R.string.shortcut_enable_short_label
            QuietModeAction.Disable -> R.string.shortcut_disable_short_label
            QuietModeAction.Toggle -> R.string.shortcut_toggle_short_label
        }
        return context.getString(stringId, profileLabel)
    }

    private fun shortcutLongLabel(action: QuietModeAction, profileLabel: String): String {
        val stringId = when (action) {
            QuietModeAction.Enable -> R.string.shortcut_enable_long_label
            QuietModeAction.Disable -> R.string.shortcut_disable_long_label
            QuietModeAction.Toggle -> R.string.shortcut_toggle_long_label
        }
        return context.getString(stringId, profileLabel)
    }
}

internal data class ShortcutUpdateState(
    val shortcutsCount: Int,
    val maxShortcuts: Int,
)

private data class ShortcutDescriptor(
    val id: String,
    val action: QuietModeAction,
    val serialNumber: Long,
    val shortLabel: String,
    val longLabel: String,
)
