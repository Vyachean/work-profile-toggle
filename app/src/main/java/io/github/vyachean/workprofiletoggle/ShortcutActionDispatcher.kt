package io.github.vyachean.workprofiletoggle

import android.content.Intent
import android.os.UserHandle

internal class ShortcutActionDispatcher(
    private val workProfileRepository: WorkProfileRepository,
    private val quietModeController: QuietModeController,
) {
    fun dispatch(intent: Intent): ShortcutDispatchResult {
        val action = QuietModeAction.fromIntentAction(intent.action)
            ?: return ShortcutDispatchResult.Ignored
        intent.action = null

        val serialNumber = if (intent.hasExtra(EXTRA_PROFILE_SERIAL)) {
            intent.getLongExtra(EXTRA_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            return ShortcutDispatchResult.MissingProfileSerial
        }

        val userHandle = workProfileRepository.findUserHandle(serialNumber)
            ?: return ShortcutDispatchResult.UnknownProfile(serialNumber)

        if (action == QuietModeAction.Toggle && quietModeController.isQuietModeEnabled(userHandle).getOrNull() == null) {
            return ShortcutDispatchResult.ToggleStateUnavailable(userHandle)
        }

        return quietModeController.requestQuietMode(userHandle, action)
            .fold(
                onSuccess = { result ->
                    ShortcutDispatchResult.Completed(
                        action = action,
                        userHandle = userHandle,
                        changed = result.changed,
                    )
                },
                onFailure = { error ->
                    ShortcutDispatchResult.Failed(
                        action = action,
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
        val action: QuietModeAction,
        val userHandle: UserHandle,
        val changed: Boolean,
    ) : ShortcutDispatchResult()

    data class Failed(
        val action: QuietModeAction,
        val error: Throwable,
    ) : ShortcutDispatchResult()
}
