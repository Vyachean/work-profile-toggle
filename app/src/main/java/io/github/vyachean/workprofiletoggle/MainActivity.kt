package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val STATE_LAST_RESULT = "last_result"
private const val MODIFY_QUIET_MODE_PERMISSION = "android.permission.MODIFY_QUIET_MODE"

class MainActivity : Activity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
    private lateinit var dependencies: WorkProfileAppDependencies
    private lateinit var actionResultStore: ActionResultStore
    private lateinit var quietModeController: QuietModeController
    private lateinit var workProfileRepository: WorkProfileRepository
    private lateinit var shortcutController: ShortcutController
    private lateinit var actionDispatcher: WorkProfileActionDispatcher
    private lateinit var content: LinearLayout
    private var lastResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dependencies = WorkProfileAppDependencies(this)
        actionResultStore = dependencies.actionResultStore
        lastResult = actionResultStore.restore(savedInstanceState?.getString(STATE_LAST_RESULT))
        quietModeController = dependencies.quietModeController
        workProfileRepository = dependencies.workProfileRepository
        shortcutController = dependencies.shortcutController
        actionDispatcher = dependencies.actionDispatcher
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

        if (savedInstanceState == null) {
            handleShortcutIntent(intent)
        }
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_LAST_RESULT, lastResult)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        val profileDiscovery = workProfileRepository.discoverProfiles()
        val profileEntries = profileDiscovery.profileEntries
        val labeledEntries = profileDiscovery.labeledEntries
        val profileSelection = workProfileRepository.resolveProfileSelection(labeledEntries)
        val primaryProfile = profileSelection.selected
        val primaryQuietMode = primaryProfile?.let { readQuietMode(it.userHandle) }
        val permissionGranted = hasQuietModePermission()
        val shortcutUpdateResult = shortcutController.updateShortcuts(labeledEntries)

        content.removeAllViews()

        content.addView(textView(getString(R.string.home_title), textSize = 22f))
        renderPrimaryStatus(profileSelection, primaryQuietMode, permissionGranted, profileDiscovery.profilesAvailable)
        renderSetup(profileSelection, permissionGranted, profileDiscovery.error)
        renderSchedulePreview()
        renderAdvanced(profileEntries, shortcutUpdateResult)
    }

    private fun renderPrimaryStatus(
        profileSelection: ProfileSelection,
        quietMode: QuietModeState?,
        permissionGranted: Boolean,
        profilesAvailable: Boolean,
    ) {
        val primaryProfile = profileSelection.selected
        when {
            !profilesAvailable || profileSelection.availableProfiles.isEmpty() -> {
                content.addView(textView(getString(R.string.no_work_profile_found_title), textSize = 18f))
                content.addView(textView(getString(R.string.no_work_profile_found_description)))
                content.addView(button(getString(R.string.check_again)) { render() })
            }
            primaryProfile == null -> {
                content.addView(textView(getString(R.string.choose_work_profile), textSize = 18f))
                content.addView(
                    textView(
                        if (profileSelection.missingSelectedProfile) {
                            getString(R.string.selected_profile_unavailable)
                        } else {
                            getString(R.string.select_profile_to_control)
                        },
                    ),
                )
                renderProfileChoices(profileSelection.availableProfiles)
                content.addView(button(getString(R.string.check_again)) { render() })
            }
            !permissionGranted -> {
                content.addView(textView(getString(R.string.setup_required), textSize = 18f))
                content.addView(textView(getString(R.string.setup_permission_message)))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.check_again)) { render() })
            }
            quietMode?.value == true -> {
                content.addView(textView(getString(R.string.work_profile_paused), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.resume_work_profile)) {
                    dispatchAction(primaryProfile.userHandle, QuietModeAction.Disable)
                    render()
                })
            }
            quietMode?.value == false -> {
                content.addView(textView(getString(R.string.work_profile_active), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.pause_work_profile)) {
                    dispatchAction(primaryProfile.userHandle, QuietModeAction.Enable)
                    render()
                })
            }
            else -> {
                content.addView(textView(getString(R.string.work_profile_unknown), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.check_again)) { render() })
            }
        }
    }

    private fun renderSetup(
        profileSelection: ProfileSelection,
        permissionGranted: Boolean,
        profilesError: Throwable?,
    ) {
        val hasProfile = profileSelection.availableProfiles.isNotEmpty()
        val hasSelectedProfile = profileSelection.selected != null
        val ready = hasSelectedProfile && permissionGranted && profilesError == null
        content.addView(sectionTitle(getString(R.string.setup_title)))
        content.addView(textView(if (ready) getString(R.string.setup_ready) else getString(R.string.setup_required)))
        content.addView(textView(if (hasProfile) getString(R.string.setup_profile_found) else getString(R.string.setup_profile_missing)))
        content.addView(
            textView(
                profileSelection.selected?.let {
                    getString(R.string.selected_profile_format, it.profile.label)
                } ?: getString(R.string.selected_profile_none),
            ),
        )
        content.addView(
            textView(
                if (permissionGranted) {
                    getString(R.string.setup_permission_granted)
                } else {
                    getString(R.string.setup_permission_missing)
                },
            ),
        )
        profilesError?.let { error ->
            content.addView(textView(formatFailure("getUserProfiles", error)))
        }
        if (!permissionGranted) {
            content.addView(sectionTitle(getString(R.string.adb_setup_title)))
            content.addView(textView(getString(R.string.adb_setup_description)))
            content.addView(textView(setupText()))
            content.addView(button(getString(R.string.copy_setup_text)) { copySetupText() })
        }
    }

    private fun renderSchedulePreview() {
        content.addView(sectionTitle(getString(R.string.schedule_title)))
        content.addView(textView(getString(R.string.schedule_not_configured)))
        content.addView(textView(getString(R.string.schedule_future_note)))
    }

    private fun renderAdvanced(
        profileEntries: List<ProfileEntry>,
        shortcutUpdateResult: Result<ShortcutUpdateState?>,
    ) {
        content.addView(sectionTitle(getString(R.string.advanced_title)))
        content.addView(textView(getString(R.string.last_result, lastResult)))
        content.addView(textView(getString(R.string.profiles_found, profileEntries.size)))
        shortcutUpdateResult.getOrNull()?.let { updateState ->
            content.addView(
                textView(
                    getString(
                        R.string.shortcuts_updated,
                        updateState.shortcutsCount,
                        updateState.maxShortcuts,
                    ),
                ),
            )
        }
        shortcutUpdateResult.exceptionOrNull()?.let { error ->
            content.addView(textView(formatFailure("updateShortcuts", error)))
        }

        if (profileEntries.isEmpty()) {
            content.addView(textView(getString(R.string.no_switchable_profiles)))
        }
        profileEntries.forEach { profileEntry ->
            content.addView(profileView(profileEntry))
        }
    }

    private fun renderSelectedProfile(profileSelection: ProfileSelection) {
        val selected = profileSelection.selected ?: return
        content.addView(textView(getString(R.string.selected_profile_label, selected.profile.label)))
        if (profileSelection.availableProfiles.size > 1) {
            content.addView(button(getString(R.string.change_work_profile)) {
                workProfileRepository.clearSelectedProfile()
                render()
            })
        }
    }

    private fun renderProfileChoices(profiles: List<ProfileEntry.Labeled>) {
        profiles.forEach { profileEntry ->
            content.addView(button(getString(R.string.use_work_profile, profileEntry.profile.label)) {
                workProfileRepository.saveSelectedProfile(profileEntry)
                Toast.makeText(
                    this,
                    getString(R.string.profile_selected_toast, profileEntry.profile.label),
                    Toast.LENGTH_SHORT,
                ).show()
                render()
            })
        }
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
                dispatchAction(userHandle, QuietModeAction.Enable)
                render()
            })
            addView(button(getString(R.string.disable_quiet_mode)) {
                dispatchAction(userHandle, QuietModeAction.Disable)
                render()
            })
            addView(button(getString(R.string.toggle_quiet_mode)) {
                dispatchAction(userHandle, QuietModeAction.Toggle)
                render()
            })
        }
    }

    private fun handleShortcutIntent(intent: Intent) {
        handleActionResult(actionDispatcher.dispatchShortcut(intent))
    }

    private fun dispatchAction(userHandle: UserHandle, action: QuietModeAction) {
        handleActionResult(actionDispatcher.dispatch(userHandle, action))
    }

    private fun handleActionResult(result: WorkProfileActionResult) {
        when (result) {
            WorkProfileActionResult.Ignored -> Unit
            WorkProfileActionResult.MissingProfileSerial -> setLastResult(getString(R.string.shortcut_missing_serial))
            is WorkProfileActionResult.UnknownProfile -> setLastResult(
                getString(R.string.shortcut_unknown_profile, result.serialNumber),
            )
            is WorkProfileActionResult.Completed -> setLastResult(
                getString(
                    R.string.operation_returned,
                    operationLabel(result.requestedAction),
                    result.userHandle.toString(),
                    result.changed.toString(),
                    timestamp(),
                ),
            )
            is WorkProfileActionResult.Failed -> setLastResult(
                formatFailure(operationLabel(result.requestedAction), result.error),
            )
        }
    }

    private fun readQuietMode(userHandle: UserHandle): QuietModeState {
        return quietModeController.isQuietModeEnabled(userHandle)
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

    private fun operationLabel(action: QuietModeAction): String {
        val stringId = when (action) {
            QuietModeAction.Enable -> R.string.operation_enable_quiet_mode
            QuietModeAction.Disable -> R.string.operation_disable_quiet_mode
            QuietModeAction.Toggle -> R.string.operation_toggle_quiet_mode
        }
        return getString(stringId)
    }

    private fun hasQuietModePermission(): Boolean {
        return checkSelfPermission(MODIFY_QUIET_MODE_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupText(): String {
        return listOf(
            "adb",
            "shell",
            "pm",
            "grant",
            packageName,
            MODIFY_QUIET_MODE_PERMISSION,
        ).joinToString(" ")
    }

    private fun copySetupText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.setup_title), setupText()))
        Toast.makeText(this, getString(R.string.setup_text_copied), Toast.LENGTH_SHORT).show()
    }

    private fun setLastResult(result: String) {
        lastResult = result
        actionResultStore.save(result)
    }

    private fun formatFailure(operation: String, error: Throwable): String {
        return dependencies.formatFailure(operation, error)
    }

    private fun sectionTitle(text: String): TextView {
        return textView(text, textSize = 16f)
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

private data class QuietModeState(
    val value: Boolean?,
    val message: String,
)
