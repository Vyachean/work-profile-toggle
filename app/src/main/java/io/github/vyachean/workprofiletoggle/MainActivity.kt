package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var userManager: UserManager
    private lateinit var content: LinearLayout
    private var lastResult: String = "No action executed yet."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userManager = getSystemService(UserManager::class.java)
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
        val profilesResult = runCatching { userManager.userProfiles }
        val profiles = profilesResult.getOrElse { emptyList() }

        content.removeAllViews()

        content.addView(
            textView(
                text = getString(R.string.app_name),
                textSize = 20f,
            ),
        )
        content.addView(textView(getString(R.string.poc_description)))
        content.addView(button(getString(R.string.refresh_profiles)) { render() })

        profilesResult.exceptionOrNull()?.let { error ->
            content.addView(textView(formatFailure("getUserProfiles", error)))
        }

        content.addView(textView("Last result:\n$lastResult"))
        content.addView(textView("Profiles found: ${profiles.size}"))

        profiles.forEachIndexed { index, userHandle ->
            content.addView(profileView(index, userHandle))
        }
    }

    private fun profileView(index: Int, userHandle: UserHandle): LinearLayout {
        val serialNumber = runCatching { userManager.getSerialNumberForUser(userHandle) }
            .fold(
                onSuccess = { it.toString() },
                onFailure = { error -> formatFailure("getSerialNumberForUser", error) },
            )

        val quietMode = readQuietMode(userHandle)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16.dp, 0, 16.dp)

            addView(
                textView(
                    "Profile ${index + 1}\n" +
                        "Handle: $userHandle\n" +
                        "Serial: $serialNumber\n" +
                        "Quiet mode: ${quietMode.message}",
                ),
            )

            addView(button(getString(R.string.enable_quiet_mode)) {
                requestQuietMode(userHandle, enableQuietMode = true)
            })
            addView(button(getString(R.string.disable_quiet_mode)) {
                requestQuietMode(userHandle, enableQuietMode = false)
            })
            addView(button(getString(R.string.toggle_quiet_mode)) {
                val currentQuietMode = readQuietMode(userHandle).value
                if (currentQuietMode == null) {
                    lastResult = "Toggle skipped: quiet-mode state is unavailable for $userHandle."
                    render()
                } else {
                    requestQuietMode(userHandle, enableQuietMode = !currentQuietMode)
                }
            })
        }
    }

    private fun readQuietMode(userHandle: UserHandle): QuietModeState {
        return runCatching { userManager.isQuietModeEnabled(userHandle) }
            .fold(
                onSuccess = { QuietModeState(value = it, message = it.toString()) },
                onFailure = { error ->
                    QuietModeState(
                        value = null,
                        message = formatFailure("isQuietModeEnabled", error),
                    )
                },
            )
    }

    private fun requestQuietMode(userHandle: UserHandle, enableQuietMode: Boolean) {
        val action = if (enableQuietMode) "enable quiet mode" else "disable quiet mode"
        lastResult = runCatching {
            val changed = if (!enableQuietMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                userManager.requestQuietModeEnabled(
                    false,
                    userHandle,
                    UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED,
                )
            } else {
                userManager.requestQuietModeEnabled(enableQuietMode, userHandle)
            }

            "$action for $userHandle returned $changed at ${timestamp()}."
        }.getOrElse { error ->
            formatFailure(action, error)
        }

        render()
    }

    private fun formatFailure(operation: String, error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.name
        return "$operation failed with ${error::class.java.simpleName}: $detail"
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

    private fun timestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()
}

private data class QuietModeState(
    val value: Boolean?,
    val message: String,
)
