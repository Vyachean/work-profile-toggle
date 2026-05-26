package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
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

private const val SHORTCUTS_PER_PROFILE = 3
private const val STATE_LAST_RESULT = "last_result"
private const val MODIFY_QUIET_MODE_PERMISSION = "android.permission.MODIFY_QUIET_MODE"
private const val PREFERENCES_NAME = "work_profile_toggle"
private const val PREF_SELECTED_PROFILE_SERIAL = "selected_profile_serial"

class MainActivity : Activity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
    private lateinit var preferences: SharedPreferences
    private lateinit var userManager: UserManager
    private var shortcutManager: ShortcutManager? = null
    private lateinit var content: LinearLayout
    private var lastResult: String = ""
    private var lastShortcutSignature: List<ShortcutDescriptor>? = null
    private var userHandlesBySerialNumber: Map<Long, UserHandle> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastResult = savedInstanceState?.getString(STATE_LAST_RESULT)
            ?: getString(R.string.no_action_executed)
        preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        shortcutManager = getSystemService(ShortcutManager::class.java)
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
        val profilesResult = runCatching { userManager.userProfiles }
        val profiles = profilesResult.getOrElse { emptyList() }
        val profileEntries = createProfileEntries(profiles)
        val labeledEntries = profileEntries.filterIsInstance<ProfileEntry.Labeled>()
        val profileSelection = resolveProfileSelection(labeledEntries)
        val primaryProfile = profileSelection.selected
        val primaryQuietMode = primaryProfile?.let { readQuietMode(it.userHandle) }
        val permissionGranted = hasQuietModePermission()
        val shortcutUpdateResult = updateShortcuts(labeledEntries)

        content.removeAllViews()

        content.addView(textView(getString(R.string.home_title), textSize = 22f))
        renderPrimaryStatus(profileSelection, primaryQuietMode, permissionGranted, profilesResult.isSuccess)
        renderSetup(profileSelection, permissionGranted, profilesResult.exceptionOrNull())
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
                    requestQuietMode(primaryProfile.userHandle, QuietModeAction.Disable)
                    render()
                })
            }
            quietMode?.value == false -> {
                content.addView(textView(getString(R.string.work_profile_active), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.pause_work_profile)) {
                    requestQuietMode(primaryProfile.userHandle, QuietModeAction.Enable)
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
                clearSelectedProfile()
                render()
            })
        }
    }

    private fun renderProfileChoices(profiles: List<ProfileEntry.Labeled>) {
        profiles.forEach { profileEntry ->
            content.addView(button(getString(R.string.use_work_profile, profileEntry.profile.label)) {
                saveSelectedProfile(profileEntry)
                Toast.makeText(
                    this,
                    getString(R.string.profile_selected_toast, profileEntry.profile.label),
                    Toast.LENGTH_SHORT,
                ).show()
                render()
            })
        }
    }

    private fun createProfileEntries(profiles: List<UserHandle>): List<ProfileEntry> {
        val handlesBySerialNumber = mutableMapOf<Long, UserHandle>()
        val diagnosticEntries = mutableListOf<ProfileEntry.Diagnostic>()

        profiles.forEach { userHandle ->
            runCatching { userManager.getSerialNumberForUser(userHandle) }
                .fold(
                    onSuccess = { serialNumber ->
                        when (serialNumber) {
                            INVALID_SERIAL_NUMBER -> {
                                diagnosticEntries += ProfileEntry.Diagnostic(
                                    userHandle = userHandle,
                                    serialDiagnostic = getString(R.string.profile_serial_invalid),
                                )
                            }
                            OWNER_PROFILE_SERIAL_NUMBER -> Unit
                            else -> handlesBySerialNumber.putIfAbsent(serialNumber, userHandle)
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
        userHandlesBySerialNumber = handlesBySerialNumber.toMap()

        val labeledEntries = ProfileLabels.fromSerialNumbers(handlesBySerialNumber.keys) { ordinal, serialNumber ->
            getString(R.string.profile_fallback_label, ordinal, serialNumber)
        }.map { profile ->
            val serialNumber = profile.identifier.serialNumber
            ProfileEntry.Labeled(
                userHandle = requireNotNull(handlesBySerialNumber[serialNumber]) {
                    "Missing UserHandle for profile serial $serialNumber"
                },
                profile = profile,
            )
        }

        return labeledEntries + diagnosticEntries.sortedBy { it.userHandle.toString() }
    }

    private fun resolveProfileSelection(profiles: List<ProfileEntry.Labeled>): ProfileSelection {
        val selectedSerialNumber = selectedProfileSerialNumber()
        val selectedProfile = selectedSerialNumber?.let { serialNumber ->
            profiles.firstOrNull { profileEntry -> profileEntry.profile.identifier.serialNumber == serialNumber }
        }

        if (selectedProfile != null) {
            return ProfileSelection(
                selected = selectedProfile,
                availableProfiles = profiles,
                missingSelectedProfile = false,
            )
        }

        if (selectedSerialNumber == null && profiles.size == 1) {
            val onlyProfile = profiles.single()
            saveSelectedProfile(onlyProfile)
            return ProfileSelection(
                selected = onlyProfile,
                availableProfiles = profiles,
                missingSelectedProfile = false,
            )
        }

        return ProfileSelection(
            selected = null,
            availableProfiles = profiles,
            missingSelectedProfile = selectedSerialNumber != null,
        )
    }

    private fun selectedProfileSerialNumber(): Long? {
        return if (preferences.contains(PREF_SELECTED_PROFILE_SERIAL)) {
            preferences.getLong(PREF_SELECTED_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            null
        }
    }

    private fun saveSelectedProfile(profileEntry: ProfileEntry.Labeled) {
        preferences.edit()
            .putLong(PREF_SELECTED_PROFILE_SERIAL, profileEntry.profile.identifier.serialNumber)
            .apply()
    }

    private fun clearSelectedProfile() {
        preferences.edit()
            .remove(PREF_SELECTED_PROFILE_SERIAL)
            .apply()
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
                requestQuietMode(userHandle, QuietModeAction.Enable)
                render()
            })
            addView(button(getString(R.string.disable_quiet_mode)) {
                requestQuietMode(userHandle, QuietModeAction.Disable)
                render()
            })
            addView(button(getString(R.string.toggle_quiet_mode)) {
                requestQuietMode(userHandle, QuietModeAction.Toggle)
                render()
            })
        }
    }

    private fun updateShortcuts(profiles: List<ProfileEntry.Labeled>): Result<ShortcutUpdateState?> {
        return runCatching {
            val manager = shortcutManager ?: return@runCatching null
            val maxShortcuts = manager.maxShortcutCountPerActivity
            val shortcutProfileCount = maxShortcuts / SHORTCUTS_PER_PROFILE
            val shortcutDescriptors = profiles
                .take(shortcutProfileCount)
                .flatMapIndexed { index, profileEntry ->
                    quietModeShortcutDescriptors(profileEntry, shortcutProfileLabel(index))
                }

            if (shortcutDescriptors != lastShortcutSignature) {
                manager.dynamicShortcuts = shortcutDescriptors.map { descriptor ->
                    ShortcutInfo.Builder(this, descriptor.id)
                        .setShortLabel(descriptor.shortLabel)
                        .setLongLabel(descriptor.longLabel)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_quiet_mode))
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
        return getString(R.string.shortcut_profile_short_label, index + 1)
    }

    private fun shortcutId(action: QuietModeAction, serialNumber: Long): String {
        return "${action.name.lowercase(Locale.ROOT)}-$serialNumber"
    }

    private fun shortcutIntent(action: QuietModeAction, serialNumber: Long): Intent {
        return Intent(this, QuietModeActionActivity::class.java).apply {
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
        return getString(stringId, profileLabel)
    }

    private fun shortcutLongLabel(action: QuietModeAction, profileLabel: String): String {
        val stringId = when (action) {
            QuietModeAction.Enable -> R.string.shortcut_enable_long_label
            QuietModeAction.Disable -> R.string.shortcut_disable_long_label
            QuietModeAction.Toggle -> R.string.shortcut_toggle_long_label
        }
        return getString(stringId, profileLabel)
    }

    private fun handleShortcutIntent(intent: Intent) {
        val action = QuietModeAction.fromIntentAction(intent.action) ?: return
        intent.action = null

        val serialNumber = if (intent.hasExtra(EXTRA_PROFILE_SERIAL)) {
            intent.getLongExtra(EXTRA_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            lastResult = getString(R.string.shortcut_missing_serial)
            return
        }
        val userHandle = findUserHandle(serialNumber)
        if (userHandle == null) {
            lastResult = getString(R.string.shortcut_unknown_profile, serialNumber)
            return
        }

        requestQuietMode(userHandle, action)
    }

    private fun findUserHandle(serialNumber: Long): UserHandle? {
        userHandlesBySerialNumber[serialNumber]?.let { userHandle ->
            return userHandle
        }

        refreshUserHandleCache()
        return userHandlesBySerialNumber[serialNumber]
    }

    private fun refreshUserHandleCache() {
        runCatching { userManager.userProfiles }
            .getOrNull()
            ?.let { profiles -> createProfileEntries(profiles) }
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

    private fun requestQuietMode(userHandle: UserHandle, action: QuietModeAction) {
        val targetQuietMode = when (action) {
            QuietModeAction.Enable -> true
            QuietModeAction.Disable -> false
            QuietModeAction.Toggle -> {
                val currentQuietMode = readQuietMode(userHandle).value
                if (currentQuietMode == null) {
                    lastResult = getString(R.string.toggle_skipped, userHandle.toString())
                    return
                }
                !currentQuietMode
            }
        }

        lastResult = runCatching {
            val changed = if (!targetQuietMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                userManager.requestQuietModeEnabled(
                    false,
                    userHandle,
                    UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED,
                )
            } else {
                userManager.requestQuietModeEnabled(targetQuietMode, userHandle)
            }

            getString(R.string.operation_returned, operationLabel(action), userHandle.toString(), changed.toString(), timestamp())
        }.getOrElse { error ->
            formatFailure(operationLabel(action), error)
        }
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

    private fun formatFailure(operation: String, error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.name
        return getString(
            R.string.operation_failed,
            operation,
            error::class.java.simpleName,
            detail,
        )
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

private data class ProfileSelection(
    val selected: ProfileEntry.Labeled?,
    val availableProfiles: List<ProfileEntry.Labeled>,
    val missingSelectedProfile: Boolean,
)

private data class QuietModeState(
    val value: Boolean?,
    val message: String,
)

private data class ShortcutDescriptor(
    val id: String,
    val action: QuietModeAction,
    val serialNumber: Long,
    val shortLabel: String,
    val longLabel: String,
)

private data class ShortcutUpdateState(
    val shortcutsCount: Int,
    val maxShortcuts: Int,
)
