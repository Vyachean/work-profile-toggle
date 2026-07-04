package io.github.vyachean.workprofiletoggle

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.UserManager

internal class WorkProfileAppDependencies(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    val preferences: SharedPreferences =
        appContext.getSharedPreferences(WORK_PROFILE_PREFERENCES_NAME, Context.MODE_PRIVATE)

    val actionResultStore: ActionResultStore = ActionResultStore(
        preferences = preferences,
        defaultResult = appContext.getString(R.string.no_action_executed),
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
