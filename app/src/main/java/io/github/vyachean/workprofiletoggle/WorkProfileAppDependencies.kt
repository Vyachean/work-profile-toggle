package io.github.vyachean.workprofiletoggle

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.UserManager

internal class WorkProfileAppDependencies(
    private val context: Context,
) {
    val preferences: SharedPreferences =
        context.getSharedPreferences(WORK_PROFILE_PREFERENCES_NAME, Context.MODE_PRIVATE)

    val userManager: UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    val quietModeController: QuietModeController =
        QuietModeController(userManager)

    val workProfileRepository: WorkProfileRepository = WorkProfileRepository(
        userManager = userManager,
        preferences = preferences,
        profileLabel = { ordinal, serialNumber ->
            context.getString(R.string.profile_fallback_label, ordinal, serialNumber)
        },
        invalidSerialDiagnostic = context.getString(R.string.profile_serial_invalid),
        formatFailure = ::formatFailure,
    )

    val shortcutActionDispatcher: ShortcutActionDispatcher = ShortcutActionDispatcher(
        workProfileRepository = workProfileRepository,
        quietModeController = quietModeController,
    )

    fun createShortcutController(): ShortcutController {
        return ShortcutController(
            context = context.applicationContext,
            shortcutManager = context.getSystemService(ShortcutManager::class.java),
        )
    }

    fun formatFailure(operation: String, error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.java.name
        return context.getString(
            R.string.operation_failed,
            operation,
            error::class.java.simpleName,
            detail,
        )
    }
}
