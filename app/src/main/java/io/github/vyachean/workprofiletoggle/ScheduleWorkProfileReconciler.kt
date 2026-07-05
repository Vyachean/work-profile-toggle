package io.github.vyachean.workprofiletoggle

import android.os.UserHandle

internal interface ScheduleWorkProfileReconciler {
    fun reconcile(expectedState: WorkProfileScheduleExpectedState): ScheduleWorkProfileReconciliation
}

internal data class ScheduleWorkProfileReconciliation(
    val selectedProfileStatus: ScheduleRuntimeProfileStatus,
    val requestedAction: ScheduleRuntimeRequestedAction,
    val actionResult: ScheduleRuntimeActionResult,
    val finalStateConfirmed: Boolean,
    val failureCategory: ScheduleRuntimeFailureCategory?,
)

internal class AndroidScheduleWorkProfileReconciler(
    private val workProfileRepository: WorkProfileRepository,
    private val quietModeController: QuietModeController,
    private val actionDispatcher: WorkProfileActionDispatcher,
) : ScheduleWorkProfileReconciler {
    override fun reconcile(expectedState: WorkProfileScheduleExpectedState): ScheduleWorkProfileReconciliation {
        val discovery = workProfileRepository.discoverProfiles()
        if (discovery.error != null) {
            return blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.UNAVAILABLE,
                failureCategory = ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE,
            )
        }

        val profileSelection = workProfileRepository.resolveProfileSelection(discovery.labeledEntries)
        val selectedProfile = profileSelection.selected ?: return blocked(
            selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
            failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
        )

        val expectedQuietMode = expectedState.toQuietModeEnabled()
        val currentQuietMode = quietModeController.isQuietModeEnabled(selectedProfile.userHandle)
            .getOrElse { error ->
                return failedBeforeAction(
                    selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                    failureCategory = classifyQuietModeReadFailure(error),
                )
            }

        if (currentQuietMode == expectedQuietMode) {
            return ScheduleWorkProfileReconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = ScheduleRuntimeRequestedAction.NONE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                failureCategory = null,
            )
        }

        val quietModeAction = expectedState.toQuietModeAction()
        val requestedAction = quietModeAction.toScheduleRequestedAction()
        return when (val actionResult = actionDispatcher.dispatch(selectedProfile.userHandle, quietModeAction)) {
            WorkProfileActionResult.Ignored -> failedAfterActionRequest(
                requestedAction = requestedAction,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            )
            WorkProfileActionResult.MissingProfileSerial -> blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            )
            is WorkProfileActionResult.UnknownProfile -> blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            )
            is WorkProfileActionResult.Failed -> failedAfterActionRequest(
                requestedAction = requestedAction,
                failureCategory = classifyQuietModeRequestFailure(actionResult.error),
            )
            is WorkProfileActionResult.Completed -> confirmFinalState(
                userHandle = selectedProfile.userHandle,
                expectedQuietMode = expectedQuietMode,
                requestedAction = requestedAction,
                quietModeAction = quietModeAction,
            )
        }
    }

    private fun confirmFinalState(
        userHandle: UserHandle,
        expectedQuietMode: Boolean,
        requestedAction: ScheduleRuntimeRequestedAction,
        quietModeAction: QuietModeAction,
    ): ScheduleWorkProfileReconciliation {
        val finalQuietMode = quietModeController.isQuietModeEnabled(userHandle)
            .getOrElse { error ->
                return ScheduleWorkProfileReconciliation(
                    selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                    requestedAction = requestedAction,
                    actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                    finalStateConfirmed = false,
                    failureCategory = classifyQuietModeReadFailure(error),
                )
            }

        if (finalQuietMode == expectedQuietMode) {
            return ScheduleWorkProfileReconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
                requestedAction = requestedAction,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
                failureCategory = null,
            )
        }

        return failedAfterActionRequest(
            requestedAction = requestedAction,
            failureCategory = quietModeAction.toUnconfirmedFailureCategory(),
        )
    }

    private fun blocked(
        selectedProfileStatus: ScheduleRuntimeProfileStatus,
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleWorkProfileReconciliation {
        return ScheduleWorkProfileReconciliation(
            selectedProfileStatus = selectedProfileStatus,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.BLOCKED,
            finalStateConfirmed = false,
            failureCategory = failureCategory,
        )
    }

    private fun failedBeforeAction(
        selectedProfileStatus: ScheduleRuntimeProfileStatus,
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleWorkProfileReconciliation {
        return ScheduleWorkProfileReconciliation(
            selectedProfileStatus = selectedProfileStatus,
            requestedAction = ScheduleRuntimeRequestedAction.NONE,
            actionResult = ScheduleRuntimeActionResult.FAILED,
            finalStateConfirmed = false,
            failureCategory = failureCategory,
        )
    }

    private fun failedAfterActionRequest(
        requestedAction: ScheduleRuntimeRequestedAction,
        failureCategory: ScheduleRuntimeFailureCategory,
    ): ScheduleWorkProfileReconciliation {
        return ScheduleWorkProfileReconciliation(
            selectedProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
            requestedAction = requestedAction,
            actionResult = ScheduleRuntimeActionResult.FAILED,
            finalStateConfirmed = false,
            failureCategory = failureCategory,
        )
    }

    private fun classifyQuietModeReadFailure(error: Throwable): ScheduleRuntimeFailureCategory {
        return when (error) {
            is SecurityException -> ScheduleRuntimeFailureCategory.PERMISSION_MISSING
            else -> ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE
        }
    }

    private fun classifyQuietModeRequestFailure(error: Throwable): ScheduleRuntimeFailureCategory {
        return when (error) {
            is SecurityException -> ScheduleRuntimeFailureCategory.PERMISSION_MISSING
            else -> ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED
        }
    }

    private fun WorkProfileScheduleExpectedState.toQuietModeEnabled(): Boolean {
        return when (this) {
            WorkProfileScheduleExpectedState.ACTIVE -> false
            WorkProfileScheduleExpectedState.PAUSED -> true
        }
    }

    private fun WorkProfileScheduleExpectedState.toQuietModeAction(): QuietModeAction {
        return when (this) {
            WorkProfileScheduleExpectedState.ACTIVE -> QuietModeAction.Disable
            WorkProfileScheduleExpectedState.PAUSED -> QuietModeAction.Enable
        }
    }

    private fun QuietModeAction.toScheduleRequestedAction(): ScheduleRuntimeRequestedAction {
        return when (this) {
            QuietModeAction.Enable -> ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE
            QuietModeAction.Disable -> ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE
            QuietModeAction.Toggle -> ScheduleRuntimeRequestedAction.NONE
        }
    }

    private fun QuietModeAction.toUnconfirmedFailureCategory(): ScheduleRuntimeFailureCategory {
        return when (this) {
            QuietModeAction.Disable -> ScheduleRuntimeFailureCategory.CREDENTIAL_REQUIRED
            QuietModeAction.Enable,
            QuietModeAction.Toggle -> ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED
        }
    }
}
