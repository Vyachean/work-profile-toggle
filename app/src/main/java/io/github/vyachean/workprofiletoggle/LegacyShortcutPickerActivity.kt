package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

class LegacyShortcutPickerActivity : Activity() {
    private lateinit var userManager: UserManager
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        setContentView(
            ScrollView(this).apply {
                addView(
                    content,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )

        render()
    }

    private fun render() {
        val profiles = discoverSwitchableProfiles()

        content.removeAllViews()
        content.addView(textView(getString(R.string.legacy_shortcut_picker_title), textSize = 20f))
        content.addView(textView(getString(R.string.legacy_shortcut_picker_description)))

        if (profiles.isEmpty()) {
            content.addView(textView(getString(R.string.legacy_shortcut_picker_empty)))
            return
        }

        profiles.forEach { profile ->
            content.addView(profileView(profile))
        }
    }

    private fun discoverSwitchableProfiles(): List<DiscoveredProfile> {
        val serialNumbers = runCatching { userManager.userProfiles }
            .getOrElse { emptyList() }
            .mapNotNull { userHandle -> userHandle.switchableSerialNumberOrNull() }

        return ProfileLabels.fromSerialNumbers(serialNumbers) { ordinal, serialNumber ->
            getString(R.string.profile_fallback_label, ordinal, serialNumber)
        }
    }

    private fun UserHandle.switchableSerialNumberOrNull(): Long? {
        val serialNumber = runCatching { userManager.getSerialNumberForUser(this) }
            .getOrNull()
            ?: return null

        return serialNumber.takeUnless { serial ->
            serial == INVALID_SERIAL_NUMBER || serial == OWNER_PROFILE_SERIAL_NUMBER
        }
    }

    private fun profileView(profile: DiscoveredProfile): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16.dp, 0, 16.dp)

            addView(textView(profile.label))
            QuietModeAction.entries.forEach { action ->
                addView(
                    button(shortcutLongLabel(action, profile.label)) {
                        finishWithShortcut(action, profile)
                    },
                )
            }
        }
    }

    private fun finishWithShortcut(action: QuietModeAction, profile: DiscoveredProfile) {
        val shortcutIntent = Intent(this, MainActivity::class.java).apply {
            this.action = action.intentAction
            putExtra(EXTRA_PROFILE_SERIAL, profile.identifier.serialNumber)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val shortcutName = shortcutLongLabel(action, profile.label)
        val result = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
            putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(
                    this@LegacyShortcutPickerActivity,
                    R.drawable.ic_shortcut_quiet_mode,
                ),
            )
        }

        setResult(RESULT_OK, result)
        finish()
    }

    private fun shortcutLongLabel(action: QuietModeAction, profileLabel: String): String {
        val stringId = when (action) {
            QuietModeAction.Enable -> R.string.shortcut_enable_long_label
            QuietModeAction.Disable -> R.string.shortcut_disable_long_label
            QuietModeAction.Toggle -> R.string.shortcut_toggle_long_label
        }
        return getString(stringId, profileLabel)
    }

    private fun textView(text: String, textSize: Float = 14f): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            setTextIsSelectable(true)
            setPadding(0, 8.dp, 0, 8.dp)
        }
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()
}
