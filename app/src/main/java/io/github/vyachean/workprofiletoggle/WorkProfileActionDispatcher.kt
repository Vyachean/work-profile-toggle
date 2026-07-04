package io.github.vyachean.workprofiletoggle

import android.content.Intent
import android.os.UserHandle

internal class WorkProfileActionDispatcher(
    private val workProfileRepository: WorkProfileRepository,
    private val quietModeController: QuietModeController,
) {
    fun dispatchShortcut(intent: Intent): WorkProfileActionResult {
        val requestedAction = QuietModeAction.fromIntentAction(intent.action)
            ?: return WorkProfileActionResult.Ignored
        intent.action = null

        val serialNumber = if (intent.hasExtra(EXTRA_PROFILE_SERIAL)) {
            intent.getLongExtra(EXTRA_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            return WorkProfileActionResult.MissingProfileSerial
        }

        val userHandle = workProfileRepository.findUserHandle(serialNumber)
            ?: return WorkProfileActionResult.UnknownProfile(serialNumber)

        return dispatch(userHandle, requestedAction)
    }

    fun dispatch(userHandle: UserHandle, requestedAction: QuietModeAction): WorkProfileActionResult {
        val executionAction = if (requestedAction == QuietModeAction.Toggle) {
            val quietModeEnabled = quietModeController.isQuietModeEnabled(userHandle).getOrNull()
                ?: return WorkProfileActionResult.ToggleStateUnavailable(userHandle)
            if (quietModeEnabled) QuietModeAction.Disable else QuietModeAction.Enable
        } else {
            requestedAction
        }

        return quietModeController.requestQuietMode(userHandle, executionAction)
            .fold(
                onSuccess = { result ->
                    WorkProfileActionResult.Completed(
                        requestedAction = requestedAction,
                        userHandle = userHandle,
                        changed = result.changed,
                    )
                },
                onFailure = { error ->
                    WorkProfileActionResult.Failed(
                        requestedAction = requestedAction,
                        error = error,
                    )
                },
            )
    }
}

internal sealed class WorkProfileActionResult {
    object Ignored : WorkProfileActionResult()
    object MissingProfileSerial : WorkProfileActionResult()

    data class UnknownProfile(
        val serialNumber: Long,
    ) : WorkProfileActionResult()

    data class ToggleStateUnavailable(
        val userHandle: UserHandle,
    ) : WorkProfileActionResult()

    data class Completed(
        val requestedAction: QuietModeAction,
        val userHandle: UserHandle,
        val changed: Boolean,
    ) : WorkProfileActionResult()

    data class Failed(
        val requestedAction: QuietModeAction,
        val error: Throwable,
    ) : WorkProfileActionResult()
}
