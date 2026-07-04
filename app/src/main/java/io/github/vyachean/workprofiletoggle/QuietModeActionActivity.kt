package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.UserManager

class QuietModeActionActivity : Activity() {
    private lateinit var shortcutActionDispatcher: ShortcutActionDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        val workProfileRepository = WorkProfileRepository(
            userManager = userManager,
            preferences = getSharedPreferences(WORK_PROFILE_PREFERENCES_NAME, Context.MODE_PRIVATE),
            profileLabel = { ordinal, serialNumber ->
                getString(R.string.profile_fallback_label, ordinal, serialNumber)
            },
            invalidSerialDiagnostic = getString(R.string.profile_serial_invalid),
            formatFailure = ::formatFailure,
        )
        val quietModeController = QuietModeController(userManager)
        shortcutActionDispatcher = ShortcutActionDispatcher(
            workProfileRepository = workProfileRepository,
            quietModeController = quietModeController,
        )

        shortcutActionDispatcher.dispatch(intent)
        finish()
        overridePendingTransition(0, 0)
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
}
