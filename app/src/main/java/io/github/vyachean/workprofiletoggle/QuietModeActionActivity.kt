package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager

class QuietModeActionActivity : Activity() {
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        handleShortcutIntent()
        finish()
        overridePendingTransition(0, 0)
    }

    private fun handleShortcutIntent() {
        val action = QuietModeAction.fromIntentAction(intent.action) ?: return
        intent.action = null

        val serialNumber = if (intent.hasExtra(EXTRA_PROFILE_SERIAL)) {
            intent.getLongExtra(EXTRA_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            return
        }
        val userHandle = findUserHandle(serialNumber) ?: return

        requestQuietMode(userHandle, action)
    }

    private fun findUserHandle(serialNumber: Long): UserHandle? {
        return runCatching { userManager.userProfiles }
            .getOrElse { emptyList() }
            .firstOrNull { userHandle ->
                runCatching { userManager.getSerialNumberForUser(userHandle) }
                    .getOrNull() == serialNumber
            }
    }

    private fun requestQuietMode(userHandle: UserHandle, action: QuietModeAction) {
        val targetQuietMode = when (action) {
            QuietModeAction.Enable -> true
            QuietModeAction.Disable -> false
            QuietModeAction.Toggle -> {
                val currentQuietMode = runCatching { userManager.isQuietModeEnabled(userHandle) }
                    .getOrNull()
                    ?: return
                !currentQuietMode
            }
        }

        runCatching {
            if (!targetQuietMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                userManager.requestQuietModeEnabled(
                    false,
                    userHandle,
                    UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED,
                )
            } else {
                userManager.requestQuietModeEnabled(targetQuietMode, userHandle)
            }
        }
    }
}
