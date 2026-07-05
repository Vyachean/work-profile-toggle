package io.github.vyachean.workprofiletoggle

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.UserManager
import java.time.Clock

internal class WorkProfileAppDependencies(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    val preferences: SharedPreferences =
        appContext.getSharedPreferences(WORK_PROFILE_PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val keyValueStore: KeyValueStore = SharedPreferencesKeyValueStore(preferences)

    val actionResultStore: ActionResultStore = ActionResultStore(
        keyValueStore = keyValueStore,
        defaultResult = appContext.getString(R.string.no_action_executed),
    )

    val scheduleStore: WorkProfileScheduleStore = WorkProfileScheduleStore(
        keyValueStore = keyValueStore,
    )

    val scheduleRuntimeResultStore: ScheduleRuntimeResultStore = ScheduleRuntimeResultStore(
        keyValueStore = keyValueStore,
    )

    val scheduleBoundaryHandler: ScheduleBoundaryHandler = ScheduleBoundaryRuntimeHandler(
        scheduleStore = scheduleStore,
        runtimeResultStore = scheduleRuntimeResultStore,
        clock = Clock.systemDefaultZone(),
    )

    val userManager: UserManager =
        appContext.getSystemService(Context.USER_SERVICE) as UserManager

    val quietModeController: QuietModeController =
        QuietModeController(userManager)

    val workProfileRepository: WorkProfileRepository = WorkProfileRepository(
        userManager = userManager,
        preferences = preferences,
        profileLabel = { ordinal, serialNumber ->
            appContext.getString(R.string.profile_fallback_label, ordinal, serialNumber)
        },
        invalidSerialDiagnostic = appContext.getString(R.string.profile_serial_invalid),
        formatFailure = ::formatFailure,
    )

    val actionDispatcher: WorkProfileActionDispatcher = WorkProfileActionDispatcher(
        workProfileRepository = workProfileRepository,
        quietModeController = quietModeController,
    )

    val shortcutController: ShortcutController by lazy {
        ShortcutController(
            context = appContext,
            shortcutManager = appContext.getSystemService(ShortcutManager::class.java),
        )
    }

    fun formatFailure(operation: String, error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.name
        return appContext.getString(
            R.string.operation_failed,
            operation,
            error::class.java.simpleName,
            detail,
        )
    }
}
