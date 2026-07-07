package io.github.vyachean.workprofiletoggle

import android.app.Activity
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
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val STATE_LAST_RESULT = "last_result"
private const val MODIFY_QUIET_MODE_PERMISSION = "android.permission.MODIFY_QUIET_MODE"

class MainActivity : Activity() {
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
    private lateinit var content: LinearLayout
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
            strings = ScheduleStringProvider { stringId, args -> getString(stringId, *args) },
            timeFormatter = ::formatScheduleTimeForDisplay,
            dateTimeFormatter = { dateTime -> ScheduleDateTimeFormatter.formatForDisplay(dateTime) },
        )
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

    override fun onResume() {
        super.onResume()
        if (exactAlarmSettingsRequested) {
            exactAlarmSettingsRequested = false
            if (::scheduleStore.isInitialized && scheduleStore.load() != WorkProfileSchedule()) {
                scheduleBoundaryPlanner.refresh()
            }
            render()
        }
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
        val schedule = scheduleStore.load()
        val scheduleRuntimeResult = scheduleRuntimeResultStore.load()
        val exactAlarmAccessState = scheduleExactAlarmAccess.state()
        val shortcutUpdateResult = shortcutController.updateShortcuts(labeledEntries)
        val homeUiState = HomeUiStateFactory.from(
            input = HomeUiStateInput(
                profilesAvailable = profileDiscovery.profilesAvailable,
                availableProfileCount = profileSelection.availableProfiles.size,
                selectedProfileLabel = primaryProfile?.profile?.label,
                selectedProfileQuietMode = primaryQuietMode?.value,
                permissionGranted = permissionGranted,
                schedule = schedule,
                exactAlarmAccessState = exactAlarmAccessState,
                scheduleRuntimeResult = scheduleRuntimeResult,
            ),
            now = ZonedDateTime.now(),
        )

        content.removeAllViews()

        content.addView(textView(getString(R.string.home_title), textSize = 22f))
        renderPrimaryStatus(homeUiState.primary, profileSelection)
        renderSetup(homeUiState.setup, profileSelection, profileDiscovery.error)
        renderSchedulePreview(homeUiState.schedule, schedule, scheduleRuntimeResult)
        renderAdvanced(profileEntries, shortcutUpdateResult)
    }

    private fun renderPrimaryStatus(
        state: HomePrimaryState,
        profileSelection: ProfileSelection,
    ) {
        val primaryProfile = profileSelection.selected
        when (state) {
            HomePrimaryState.NO_WORK_PROFILE -> {
                content.addView(textView(getString(R.string.no_work_profile_found_title), textSize = 18f))
                content.addView(textView(getString(R.string.no_work_profile_found_description)))
                content.addView(button(getString(R.string.check_again)) { render() })
            }
            HomePrimaryState.CHOOSE_WORK_PROFILE -> {
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
            HomePrimaryState.SETUP_REQUIRED -> {
                content.addView(textView(getString(R.string.setup_required), textSize = 18f))
                content.addView(textView(getString(R.string.setup_permission_message)))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.check_again)) { render() })
            }
            HomePrimaryState.WORK_PROFILE_PAUSED -> {
                val selectedProfile = requireNotNull(primaryProfile)
                content.addView(textView(getString(R.string.work_profile_paused), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.resume_work_profile)) {
                    dispatchAction(selectedProfile.userHandle, QuietModeAction.Disable)
                    render()
                })
            }
            HomePrimaryState.WORK_PROFILE_ACTIVE -> {
                val selectedProfile = requireNotNull(primaryProfile)
                content.addView(textView(getString(R.string.work_profile_active), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.pause_work_profile)) {
                    dispatchAction(selectedProfile.userHandle, QuietModeAction.Enable)
                    render()
                })
            }
            HomePrimaryState.WORK_PROFILE_UNKNOWN -> {
                content.addView(textView(getString(R.string.work_profile_unknown), textSize = 18f))
                renderSelectedProfile(profileSelection)
                content.addView(button(getString(R.string.check_again)) { render() })
            }
        }
    }

    private fun renderSetup(
        setup: HomeSetupState,
        profileSelection: ProfileSelection,
        profilesError: Throwable?,
    ) {
        content.addView(sectionTitle(getString(R.string.setup_title)))
        content.addView(textView(if (setup.ready) getString(R.string.setup_ready) else getString(R.string.setup_required)))
        content.addView(textView(if (setup.profileFound) getString(R.string.setup_profile_found) else getString(R.string.setup_profile_missing)))
        content.addView(
            textView(
                setup.selectedProfileLabel?.let { label ->
                    getString(R.string.selected_profile_format, label)
                } ?: getString(R.string.selected_profile_none),
            ),
        )
        content.addView(
            textView(
                if (setup.permissionGranted) {
                    getString(R.string.setup_permission_granted)
                } else {
                    getString(R.string.setup_permission_missing)
                },
            ),
        )
        profilesError?.let { error ->
            content.addView(textView(formatFailure("getUserProfiles", error)))
        }
        if (!setup.permissionGranted) {
            content.addView(sectionTitle(getString(R.string.adb_setup_title)))
            content.addView(textView(getString(R.string.adb_setup_description)))
            content.addView(textView(setupText()))
            content.addView(button(getString(R.string.copy_setup_text)) { copySetupText() })
        }
    }

    private fun renderSchedulePreview(
        state: HomeScheduleUiState,
        schedule: WorkProfileSchedule,
        runtimeResult: ScheduleRuntimeResult?,
    ) {
        content.addView(sectionTitle(getString(R.string.schedule_title)))
        if (!state.configured) {
            content.addView(textView(getString(R.string.schedule_not_configured)))
        } else {
            content.addView(textView(scheduleTextFormatter.savedStateLabel(state.savedState)))
            renderScheduleExactAlarmAccess(state.exactAlarmAccessState)
            content.addView(textView(getString(R.string.schedule_pause_at, scheduleTextFormatter.time(schedule.pauseAt))))
            content.addView(textView(getString(R.string.schedule_resume_at, scheduleTextFormatter.time(schedule.resumeAt))))
            content.addView(textView(getString(R.string.schedule_active_days, scheduleTextFormatter.days(schedule.activeDays))))
            renderScheduleRuntimeStatus(state.runtimeStatus)
            if (state.canCopyDiagnostics) {
                content.addView(button(getString(R.string.copy_schedule_diagnostics)) {
                    copyScheduleDiagnostics(schedule, runtimeResult, state.exactAlarmAccessState)
                })
            }
        }
        renderScheduleControls(state.editor, schedule)
        content.addView(textView(getString(R.string.schedule_future_note)))
    }

    private fun renderScheduleExactAlarmAccess(state: ScheduleExactAlarmAccessState) {
        content.addView(textView(scheduleTextFormatter.exactAlarmAccessLabel(state)))

        if (state == ScheduleExactAlarmAccessState.MISSING) {
            content.addView(textView(getString(R.string.schedule_exact_alarm_missing_description)))
            content.addView(button(getString(R.string.schedule_open_app_settings)) {
                requestScheduleExactAlarmAccess()
            })
        }
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

    private fun renderScheduleRuntimeStatus(status: ScheduleRuntimeStatusSummary?) {
        status ?: return

        status.nextAction?.let { nextAction ->
            content.addView(textView(scheduleTextFormatter.nextAction(nextAction)))
        }

        status.issue?.let { issue ->
            content.addView(textView(scheduleTextFormatter.runtimeIssue(issue)))
        }
    }

    private fun renderScheduleControls(
        state: ScheduleEditorUiState,
        schedule: WorkProfileSchedule,
    ) {
        content.addView(button(getString(R.string.schedule_set_pause_time)) {
            showScheduleTimePicker(
                title = getString(R.string.schedule_set_pause_time),
                initialTime = state.pauseInitialTime,
            ) { selectedTime ->
                saveSchedule(schedule.copy(pauseAt = selectedTime))
            }
        })
        content.addView(button(getString(R.string.schedule_set_resume_time)) {
            showScheduleTimePicker(
                title = getString(R.string.schedule_set_resume_time),
                initialTime = state.resumeInitialTime,
            ) { selectedTime ->
                saveSchedule(schedule.copy(resumeAt = selectedTime))
            }
        })
        content.addView(button(getString(R.string.schedule_choose_active_days)) {
            showScheduleDaysPicker(schedule)
        })
        state.enableToggle?.let { toggle ->
            content.addView(
                button(
                    when (toggle.action) {
                        ScheduleEditorEnableToggleAction.ENABLE -> getString(R.string.schedule_enable)
                        ScheduleEditorEnableToggleAction.DISABLE -> getString(R.string.schedule_disable)
                    },
                ) {
                    saveSchedule(
                        schedule.copy(
                            enabled = toggle.action == ScheduleEditorEnableToggleAction.ENABLE,
                        ),
                    )
                },
            )
        } ?: run {
            if (state.showEnableRequirements) {
                content.addView(textView(getString(R.string.schedule_enable_requirements)))
            }
        }
        if (state.canClear) {
            content.addView(button(getString(R.string.schedule_clear)) {
                clearSchedule()
            })
        }
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
        scheduleStore.save(normalizeSchedule(schedule))
        scheduleBoundaryPlanner.refresh()
        Toast.makeText(this, getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun clearSchedule() {
        scheduleStore.clear()
        scheduleBoundaryPlanner.cancel()
        Toast.makeText(this, getString(R.string.schedule_cleared), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun normalizeSchedule(schedule: WorkProfileSchedule): WorkProfileSchedule {
        return if (isScheduleComplete(schedule)) {
            schedule
        } else {
            schedule.copy(enabled = false)
        }
    }

    private fun isScheduleComplete(schedule: WorkProfileSchedule): Boolean {
        return schedule.pauseAt != null && schedule.resumeAt != null && schedule.activeDays.isNotEmpty()
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

    private fun formatScheduleTimeForDisplay(scheduleTime: ScheduleTime): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduleTime.hour)
            set(Calendar.MINUTE, scheduleTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DateFormat.getTimeFormat(this).format(calendar.time)
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
