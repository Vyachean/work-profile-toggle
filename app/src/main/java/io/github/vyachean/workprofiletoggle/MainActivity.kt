package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val INVALID_SERIAL_NUMBER = -1L
private const val STATE_LAST_RESULT = "last_result"

class MainActivity : Activity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
    private lateinit var userManager: UserManager
    private lateinit var content: LinearLayout
    private var lastResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastResult = savedInstanceState?.getString(STATE_LAST_RESULT)
            ?: getString(R.string.no_action_executed)
        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        setContentView(
            ScrollView(this).apply {
                id = R.id.profile_scroll
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_LAST_RESULT, lastResult)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        val profilesResult = runCatching { userManager.userProfiles }
        val profiles = profilesResult.getOrElse { emptyList() }
        val profileEntries = createProfileEntries(profiles)

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

        content.addView(textView(getString(R.string.last_result, lastResult)))
        content.addView(textView(getString(R.string.profiles_found, profileEntries.size)))

        profileEntries.forEach { profileEntry ->
            content.addView(profileView(profileEntry))
        }
    }

    private fun createProfileEntries(profiles: List<UserHandle>): List<ProfileEntry> {
        val handlesBySerialNumber = mutableMapOf<Long, UserHandle>()
        val diagnosticEntries = mutableListOf<ProfileEntry.Diagnostic>()

        profiles.forEach { userHandle ->
            runCatching { userManager.getSerialNumberForUser(userHandle) }
                .fold(
                    onSuccess = { serialNumber ->
                        if (serialNumber == INVALID_SERIAL_NUMBER) {
                            diagnosticEntries += ProfileEntry.Diagnostic(
                                userHandle = userHandle,
                                serialDiagnostic = getString(R.string.profile_serial_invalid),
                            )
                        } else {
                            handlesBySerialNumber.putIfAbsent(serialNumber, userHandle)
                        }
                    },
                    onFailure = { error ->
                        diagnosticEntries += ProfileEntry.Diagnostic(
                            userHandle = userHandle,
                            serialDiagnostic = formatFailure("getSerialNumberForUser", error),
                        )
                    },
                )
        }

        val labeledEntries = ProfileLabels.fromSerialNumbers(handlesBySerialNumber.keys) { ordinal, serialNumber ->
            getString(R.string.profile_fallback_label, ordinal, serialNumber)
        }.map { profile ->
            ProfileEntry.Labeled(
                userHandle = handlesBySerialNumber.getValue(profile.identifier.serialNumber),
                profile = profile,
            )
        }

        return labeledEntries + diagnosticEntries.sortedBy { it.userHandle.toString() }
    }

    private fun profileView(profileEntry: ProfileEntry): LinearLayout {
        val userHandle = profileEntry.userHandle
        val quietMode = readQuietMode(userHandle)
        val profileInfo = when (profileEntry) {
            is ProfileEntry.Labeled -> getString(
                R.string.profile_info,
                profileEntry.profile.label,
                userHandle.toString(),
                profileEntry.profile.identifier.serialNumber,
                quietMode.message,
            )
            is ProfileEntry.Diagnostic -> getString(
                R.string.profile_info_with_serial_diagnostic,
                getString(R.string.profile_serial_diagnostic_label),
                userHandle.toString(),
                profileEntry.serialDiagnostic,
                quietMode.message,
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16.dp, 0, 16.dp)

            addView(textView(profileInfo))

            addView(button(getString(R.string.enable_quiet_mode)) {
                requestQuietMode(userHandle, enableQuietMode = true)
            })
            addView(button(getString(R.string.disable_quiet_mode)) {
                requestQuietMode(userHandle, enableQuietMode = false)
            })
            addView(button(getString(R.string.toggle_quiet_mode)) {
                val currentQuietMode = readQuietMode(userHandle).value
                if (currentQuietMode == null) {
                    lastResult = getString(R.string.toggle_skipped, userHandle.toString())
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
        val action = getString(
            if (enableQuietMode) {
                R.string.operation_enable_quiet_mode
            } else {
                R.string.operation_disable_quiet_mode
            },
        )
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

            getString(R.string.operation_returned, action, userHandle.toString(), changed.toString(), timestamp())
        }.getOrElse { error ->
            formatFailure(action, error)
        }

        render()
    }

    private fun formatFailure(operation: String, error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.name
        return getString(
            R.string.operation_failed,
            operation,
            error::class.java.simpleName,
            detail,
        )
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
        return timeFormat.format(Date())
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()
}

private sealed class ProfileEntry {
    abstract val userHandle: UserHandle

    data class Labeled(
        override val userHandle: UserHandle,
        val profile: DiscoveredProfile,
    ) : ProfileEntry()

    data class Diagnostic(
        override val userHandle: UserHandle,
        val serialDiagnostic: String,
    ) : ProfileEntry()
}

private data class QuietModeState(
    val value: Boolean?,
    val message: String,
)
