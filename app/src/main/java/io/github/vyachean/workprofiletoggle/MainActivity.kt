package io.github.vyachean.workprofiletoggle

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.vyachean.workprofiletoggle.ui.HomeScreenActions
import io.github.vyachean.workprofiletoggle.ui.HomeScreenRoute
import io.github.vyachean.workprofiletoggle.ui.WorkProfileToggleTheme
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val STATE_LAST_RESULT = "last_result"
private const val MODIFY_QUIET_MODE_PERMISSION = "android.permission.MODIFY_QUIET_MODE"

class MainActivity : ComponentActivity() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
    private lateinit var dependencies: WorkProfileAppDependencies
    private lateinit var actionResultStore: ActionResultStore
    private lateinit var scheduleStore: WorkProfileScheduleStore
    private lateinit var scheduleRuntimeResultStore: ScheduleRuntimeResultStore
    private lateinit var scheduleBoundaryPlanner: ScheduleBoundaryPlanner
    private lateinit var scheduleExactAlarmAccess: AndroidScheduleExactAlarmAccess
    private lateinit var scheduleTextFormatter: ScheduleUiTextFormatter
    private lateinit var quietModeController: QuietModeController
    private lateinit var workProfileRepository: WorkProfileRepository
    private lateinit var shortcutController: ShortcutController
    private lateinit var actionDispatcher: WorkProfileActionDispatcher
    private var screenContent by mutableStateOf<MainScreenContent?>(null)
    private var lastResult: String = ""
    private var exactAlarmSettingsRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dependencies = WorkProfileAppDependencies(this)
        actionResultStore = dependencies.actionResultStore
        scheduleStore = dependencies.scheduleStore
        scheduleRuntimeResultStore = dependencies.scheduleRuntimeResultStore
        scheduleBoundaryPlanner = dependencies.scheduleBoundaryPlanner
        scheduleExactAlarmAccess = AndroidScheduleExactAlarmAccess(this)
        scheduleTextFormatter = ScheduleUiTextFormatter(
            strings = object : ScheduleStringProvider {
                override fun get(stringId: Int): String = getString(stringId)
                override fun get(stringId: Int, vararg args: Any): String = getString(stringId, *args)
            },
            timeFormatter = ::formatScheduleTimeForDisplay,
            dateTimeFormatter = ::formatScheduleDateTimeForDisplay,
        )
        lastResult = actionResultStore.restore(savedInstanceState?.getString(STATE_LAST_RESULT))
        quietModeController = dependencies.quietModeController
        workProfileRepository = dependencies.workProfileRepository
        shortcutController = dependencies.shortcutController
        actionDispatcher = dependencies.actionDispatcher

        setContent {
            WorkProfileToggleTheme {
                screenContent?.let { content ->
                    HomeScreenRoute(
                        state = content.state,
                        actions = content.actions,
                    )
                }
            }
        }

        if (savedInstanceState == null) {
            handleShortcutIntent(intent)
        }
        refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (!::scheduleStore.isInitialized) return

        if (exactAlarmSettingsRequested) {
            exactAlarmSettingsRequested = false
            if (scheduleStore.load() != WorkProfileSchedule()) {
                scheduleBoundaryPlanner.refresh()
            }
        }
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_LAST_RESULT, lastResult)
        super.onSaveInstanceState(outState)
    }

    private fun refresh() {
        val profileDiscovery = workProfileRepository.discoverProfiles()
        val profileSelection = workProfileRepository.resolveProfileSelection(profileDiscovery.labeledEntries)
        val selectedProfile = profileSelection.selected
        val selectedQuietMode = selectedProfile?.let { readQuietMode(it.userHandle) }
        val schedule = scheduleStore.load()
        val runtimeResult = scheduleRuntimeResultStore.load()
        val exactAlarmAccessState = scheduleExactAlarmAccess.state()
        val shortcutUpdateResult = shortcutController.updateShortcuts(profileDiscovery.labeledEntries)
        val homeUiState = HomeUiStateFactory.from(
            input = HomeUiStateInput(
                profilesAvailable = profileDiscovery.profilesAvailable,
                availableProfileCount = profileSelection.availableProfiles.size,
                selectedProfileLabel = selectedProfile?.profile?.label,
                selectedProfileQuietMode = selectedQuietMode?.value,
                permissionGranted = hasQuietModePermission(),
                schedule = schedule,
                exactAlarmAccessState = exactAlarmAccessState,
                scheduleRuntimeResult = runtimeResult,
            ),
            now = ZonedDateTime.now(),
        )
        val snapshot = RuntimeSnapshot(
            state = homeUiState,
            profileSelection = profileSelection,
            profileEntries = profileDiscovery.profileEntries,
            profileDiscoveryError = profileDiscovery.error,
            shortcutUpdateResult = shortcutUpdateResult,
            schedule = schedule,
            runtimeResult = runtimeResult,
            exactAlarmAccessState = exactAlarmAccessState,
        )

        screenContent = MainScreenContent(
            state = homeUiState,
            actions = createHomeScreenActions(snapshot),
        )
    }

    private fun createHomeScreenActions(snapshot: RuntimeSnapshot): HomeScreenActions {
        val selectedProfile = snapshot.profileSelection.selected
        val editorState = snapshot.state.schedule.editor

        return object : HomeScreenActions {
            override fun checkAgain() {
                refresh()
            }

            override fun pauseWorkProfile() {
                selectedProfile?.let { profile ->
                    dispatchAction(profile.userHandle, QuietModeAction.Enable)
                }
                refresh()
            }

            override fun resumeWorkProfile() {
                selectedProfile?.let { profile ->
                    dispatchAction(profile.userHandle, QuietModeAction.Disable)
                }
                refresh()
            }

            override fun changeProfile() {
                showProfilePicker(snapshot.profileSelection.availableProfiles)
            }

            override fun copySetupText() {
                this@MainActivity.copySetupText()
            }

            override fun setPauseTime() {
                showScheduleTimePicker(
                    title = getString(R.string.schedule_set_pause_time),
                    initialTime = editorState.pauseInitialTime,
                ) { selectedTime ->
                    saveSchedule(snapshot.schedule.copy(pauseAt = selectedTime))
                }
            }

            override fun setResumeTime() {
                showScheduleTimePicker(
                    title = getString(R.string.schedule_set_resume_time),
                    initialTime = editorState.resumeInitialTime,
                ) { selectedTime ->
                    saveSchedule(snapshot.schedule.copy(resumeAt = selectedTime))
                }
            }

            override fun chooseActiveDays() {
                showScheduleDaysPicker(snapshot.schedule)
            }

            override fun enableSchedule() {
                saveSchedule(snapshot.schedule.copy(enabled = true))
            }

            override fun disableSchedule() {
                saveSchedule(snapshot.schedule.copy(enabled = false))
            }

            override fun openExactAlarmSettings() {
                requestScheduleExactAlarmAccess()
            }

            override fun clearSchedule() {
                this@MainActivity.clearSchedule()
            }

            override fun copyDiagnostics() {
                copyScheduleDiagnostics(
                    schedule = snapshot.schedule,
                    runtimeResult = snapshot.runtimeResult,
                    exactAlarmAccessState = snapshot.exactAlarmAccessState,
                )
            }

            override fun showAdvanced() {
                showAdvancedDialog(snapshot)
            }
        }
    }

    private fun showProfilePicker(profiles: List<ProfileEntry.Labeled>) {
        if (profiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_switchable_profiles), Toast.LENGTH_SHORT).show()
            return
        }

        val labels = profiles.map { it.profile.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.home_screen_choose_work_profile_title))
            .setItems(labels) { _, index ->
                val selectedProfile = profiles[index]
                workProfileRepository.saveSelectedProfile(selectedProfile)
                Toast.makeText(
                    this,
                    getString(R.string.profile_selected_toast, selectedProfile.profile.label),
                    Toast.LENGTH_SHORT,
                ).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showScheduleTimePicker(
        title: String,
        initialTime: ScheduleTime,
        onTimeSelected: (ScheduleTime) -> Unit,
    ) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                onTimeSelected(ScheduleTime(hour = hourOfDay, minute = minute))
            },
            initialTime.hour,
            initialTime.minute,
            DateFormat.is24HourFormat(this),
        ).apply {
            setTitle(title)
            show()
        }
    }

    private fun showScheduleDaysPicker(schedule: WorkProfileSchedule) {
        val orderedDays = ScheduleDay.values().toList()
        val selectedDays = schedule.activeDays.toMutableSet()
        val labels = orderedDays.map { day -> scheduleTextFormatter.dayLabel(day) }.toTypedArray()
        val checkedItems = orderedDays.map { day -> day in selectedDays }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.schedule_choose_active_days))
            .setMultiChoiceItems(labels, checkedItems) { _, index, isChecked ->
                val day = orderedDays[index]
                if (isChecked) {
                    selectedDays.add(day)
                } else {
                    selectedDays.remove(day)
                }
            }
            .setPositiveButton(getString(R.string.schedule_save)) { _, _ ->
                saveSchedule(schedule.copy(activeDays = selectedDays.toSet()))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveSchedule(schedule: WorkProfileSchedule) {
        scheduleStore.save(ScheduleSavePolicy.normalizeForSave(schedule))
        scheduleBoundaryPlanner.refresh()
        Toast.makeText(this, getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun clearSchedule() {
        scheduleStore.clear()
        scheduleBoundaryPlanner.cancel()
        Toast.makeText(this, getString(R.string.schedule_cleared), Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun requestScheduleExactAlarmAccess() {
        exactAlarmSettingsRequested = true
        if (!scheduleExactAlarmAccess.openAppSettings()) {
            exactAlarmSettingsRequested = false
            Toast.makeText(
                this,
                getString(R.string.schedule_exact_alarm_settings_unavailable),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun showAdvancedDialog(snapshot: RuntimeSnapshot) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.diagnostics_title))
            .setMessage(buildAdvancedDiagnostics(snapshot))
            .setPositiveButton(android.R.string.ok, null)
            .apply {
                if (snapshot.profileEntries.isNotEmpty()) {
                    setNeutralButton(getString(R.string.advanced_title)) { _, _ ->
                        showAdvancedProfilePicker(snapshot.profileEntries)
                    }
                }
            }
            .show()
    }

    private fun showAdvancedProfilePicker(profiles: List<ProfileEntry>) {
        val labels = profiles.map(::profileDisplayLabel).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.advanced_title))
            .setItems(labels) { _, index ->
                showAdvancedProfileActions(profiles[index])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAdvancedProfileActions(profile: ProfileEntry) {
        val actions = arrayOf(
            getString(R.string.enable_quiet_mode),
            getString(R.string.disable_quiet_mode),
            getString(R.string.toggle_quiet_mode),
        )
        AlertDialog.Builder(this)
            .setTitle(profileDisplayLabel(profile))
            .setItems(actions) { _, index ->
                val action = when (index) {
                    0 -> QuietModeAction.Enable
                    1 -> QuietModeAction.Disable
                    else -> QuietModeAction.Toggle
                }
                dispatchAction(profile.userHandle, action)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun buildAdvancedDiagnostics(snapshot: RuntimeSnapshot): String {
        return buildString {
            append(getString(R.string.last_result, lastResult))
            append("\n\n")
            append(getString(R.string.profiles_found, snapshot.profileEntries.size))

            snapshot.profileDiscoveryError?.let { error ->
                append("\n\n")
                append(formatFailure("getUserProfiles", error))
            }
            snapshot.shortcutUpdateResult.getOrNull()?.let { updateState ->
                append("\n\n")
                append(
                    getString(
                        R.string.shortcuts_updated,
                        updateState.shortcutsCount,
                        updateState.maxShortcuts,
                    ),
                )
            }
            snapshot.shortcutUpdateResult.exceptionOrNull()?.let { error ->
                append("\n\n")
                append(formatFailure("updateShortcuts", error))
            }
            if (snapshot.profileEntries.isEmpty()) {
                append("\n\n")
                append(getString(R.string.no_switchable_profiles))
            }
            snapshot.profileEntries.forEach { profile ->
                append("\n\n")
                append(profileInfo(profile))
            }
        }
    }

    private fun profileDisplayLabel(profileEntry: ProfileEntry): String {
        return when (profileEntry) {
            is ProfileEntry.Labeled -> profileEntry.profile.label
            is ProfileEntry.Diagnostic -> getString(R.string.profile_serial_diagnostic_label)
        }
    }

    private fun profileInfo(profileEntry: ProfileEntry): String {
        val userHandle = profileEntry.userHandle
        val quietMode = readQuietMode(userHandle)
        return when (profileEntry) {
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

    private fun formatScheduleTimeForDisplay(scheduleTime: ScheduleTime): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduleTime.hour)
            set(Calendar.MINUTE, scheduleTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DateFormat.getTimeFormat(this).format(calendar.time)
    }

    private fun formatScheduleDateTimeForDisplay(dateTime: ZonedDateTime): String {
        return java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM,
            java.text.DateFormat.SHORT,
        ).format(Date.from(dateTime.toInstant()))
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

    private fun copyScheduleDiagnostics(
        schedule: WorkProfileSchedule,
        runtimeResult: ScheduleRuntimeResult?,
        exactAlarmAccessState: ScheduleExactAlarmAccessState,
    ) {
        val diagnostics = ScheduleRuntimeDiagnosticsFormatter.format(
            appVersionName = BuildConfig.VERSION_NAME,
            currentTime = ZonedDateTime.now(),
            schedule = schedule,
            exactAlarmAccessState = exactAlarmAccessState,
            runtimeResult = runtimeResult,
        )
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.schedule_diagnostics_clip_label),
                diagnostics,
            ),
        )
        Toast.makeText(this, getString(R.string.schedule_diagnostics_copied), Toast.LENGTH_SHORT).show()
    }

    private fun setLastResult(result: String) {
        lastResult = result
        actionResultStore.save(result)
    }

    private fun formatFailure(operation: String, error: Throwable): String {
        return dependencies.formatFailure(operation, error)
    }

    private fun timestamp(): String {
        return timeFormat.format(Date())
    }
}

private data class MainScreenContent(
    val state: HomeUiState,
    val actions: HomeScreenActions,
)

private data class RuntimeSnapshot(
    val state: HomeUiState,
    val profileSelection: ProfileSelection,
    val profileEntries: List<ProfileEntry>,
    val profileDiscoveryError: Throwable?,
    val shortcutUpdateResult: Result<ShortcutUpdateState?>,
    val schedule: WorkProfileSchedule,
    val runtimeResult: ScheduleRuntimeResult?,
    val exactAlarmAccessState: ScheduleExactAlarmAccessState,
)

private data class QuietModeState(
    val value: Boolean?,
    val message: String,
)
