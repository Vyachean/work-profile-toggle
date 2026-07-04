package io.github.vyachean.workprofiletoggle

import android.content.Intent
import android.os.UserHandle

internal class ShortcutActionDispatcher(
    private val workProfileRepository: WorkProfileRepository,
    private val quietModeController: QuietModeController,
) {
    fun dispatch(intent: Intent): ShortcutDispatchResult {
        val requestedAction = QuietModeAction.fromIntentAction(intent.action)
            ?: return ShortcutDispatchResult.Ignored
        intent.action = null

        val serialNumber = if (intent.hasExtra(EXTRA_PROFILE_SERIAL)) {
            intent.getLongExtra(EXTRA_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            return ShortcutDispatchResult.MissingProfileSerial
        }

        val userHandle = workProfileRepository.findUserHandle(serialNumber)
            ?: return ShortcutDispatchResult.UnknownProfile(serialNumber)

        val executionAction = if (requestedAction == QuietModeAction.Toggle) {
            val quietModeEnabled = quietModeController.isQuietModeEnabled(userHandle).getOrNull()
                ?: return ShortcutDispatchResult.ToggleStateUnavailable(userHandle)
            if (quietModeEnabled) QuietModeAction.Disable else QuietModeAction.Enable
        } else {
            requestedAction
        }

        return quietModeController.requestQuietMode(userHandle, executionAction)
            .fold(
                onSuccess = { result ->
                    ShortcutDispatchResult.Completed(
                        requestedAction = requestedAction,
                        userHandle = userHandle,
                        changed = result.changed,
                    )
                },
                onFailure = { error ->
                    ShortcutDispatchResult.Failed(
                        requestedAction = requestedAction,
                        error = error,
                    )
                },
            )
    }
}

internal sealed class ShortcutDispatchResult {
    object Ignored : ShortcutDispatchResult()
    object MissingProfileSerial : ShortcutDispatchResult()

    data class UnknownProfile(
        val serialNumber: Long,
    ) : ShortcutDispatchResult()

    data class ToggleStateUnavailable(
        val userHandle: UserHandle,
    ) : ShortcutDispatchResult()

    data class Completed(
        val requestedAction: QuietModeAction,
        val userHandle: UserHandle,
        val changed: Boolean,
    ) : ShortcutDispatchResult()

    data class Failed(
        val requestedAction: QuietModeAction,
        val error: Throwable,
    ) : ShortcutDispatchResult()
}
