package io.github.vyachean.workprofiletoggle

import android.os.Build
import android.os.UserHandle
import android.os.UserManager

class QuietModeController(
    private val userManager: UserManager,
) {
    fun isQuietModeEnabled(userHandle: UserHandle): Result<Boolean> {
        return runCatching { userManager.isQuietModeEnabled(userHandle) }
    }

    fun requestQuietMode(userHandle: UserHandle, action: QuietModeAction): Result<QuietModeOperationResult> {
        return runCatching {
            val targetQuietMode = when (action) {
                QuietModeAction.Enable -> true
                QuietModeAction.Disable -> false
                QuietModeAction.Toggle -> {
                    val currentQuietMode = userManager.isQuietModeEnabled(userHandle)
                    !currentQuietMode
                }
            }

            val changed = if (!targetQuietMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                userManager.requestQuietModeEnabled(
                    false,
                    userHandle,
                    UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED,
                )
            } else {
                userManager.requestQuietModeEnabled(targetQuietMode, userHandle)
            }

            QuietModeOperationResult(changed = changed)
        }
    }
}

data class QuietModeOperationResult(
    val changed: Boolean,
)
