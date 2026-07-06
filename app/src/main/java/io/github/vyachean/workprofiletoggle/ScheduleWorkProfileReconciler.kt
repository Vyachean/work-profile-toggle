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

internal interface ScheduleWorkProfileHandle

internal sealed class ScheduleWorkProfileResolution {
    data class Selected(
        val handle: ScheduleWorkProfileHandle,
    ) : ScheduleWorkProfileResolution()

    object Missing : ScheduleWorkProfileResolution()
    object Unavailable : ScheduleWorkProfileResolution()
}

internal sealed class ScheduleWorkProfileDispatchResult {
    object Completed : ScheduleWorkProfileDispatchResult()
    object Ignored : ScheduleWorkProfileDispatchResult()
    object MissingProfile : ScheduleWorkProfileDispatchResult()

    data class Failed(
        val error: Throwable,
    ) : ScheduleWorkProfileDispatchResult()
}

internal interface ScheduleWorkProfileController {
    fun resolveSelectedProfile(): ScheduleWorkProfileResolution
    fun isQuietModeEnabled(handle: ScheduleWorkProfileHandle): Result<Boolean>
    fun dispatch(handle: ScheduleWorkProfileHandle, requestedAction: QuietModeAction): ScheduleWorkProfileDispatchResult
}

private data class AndroidScheduleWorkProfileHandle(
    val userHandle: UserHandle,
) : ScheduleWorkProfileHandle

internal class AndroidScheduleWorkProfileController(
    private val workProfileRepository: WorkProfileRepository,
    private val quietModeController: QuietModeController,
    private val actionDispatcher: WorkProfileActionDispatcher,
) : ScheduleWorkProfileController {
    override fun resolveSelectedProfile(): ScheduleWorkProfileResolution {
        val discovery = workProfileRepository.discoverProfiles()
        if (discovery.error != null) {
            return ScheduleWorkProfileResolution.Unavailable
        }

        val profileSelection = workProfileRepository.resolveProfileSelection(discovery.labeledEntries)
        val selectedProfile = profileSelection.selected ?: return ScheduleWorkProfileResolution.Missing

        return ScheduleWorkProfileResolution.Selected(AndroidScheduleWorkProfileHandle(selectedProfile.userHandle))
    }

    override fun isQuietModeEnabled(handle: ScheduleWorkProfileHandle): Result<Boolean> {
        return quietModeController.isQuietModeEnabled(handle.requireAndroidHandle().userHandle)
    }

    override fun dispatch(
        handle: ScheduleWorkProfileHandle,
        requestedAction: QuietModeAction,
    ): ScheduleWorkProfileDispatchResult {
        return when (val result = actionDispatcher.dispatch(handle.requireAndroidHandle().userHandle, requestedAction)) {
            WorkProfileActionResult.Ignored -> ScheduleWorkProfileDispatchResult.Ignored
            WorkProfileActionResult.MissingProfileSerial -> ScheduleWorkProfileDispatchResult.MissingProfile
            is WorkProfileActionResult.UnknownProfile -> ScheduleWorkProfileDispatchResult.MissingProfile
            is WorkProfileActionResult.Failed -> ScheduleWorkProfileDispatchResult.Failed(result.error)
            is WorkProfileActionResult.Completed -> ScheduleWorkProfileDispatchResult.Completed
        }
    }

    private fun ScheduleWorkProfileHandle.requireAndroidHandle(): AndroidScheduleWorkProfileHandle {
        return requireNotNull(this as? AndroidScheduleWorkProfileHandle) {
            "Unsupported schedule work profile handle: ${this::class.java.name}"
        }
    }
}

internal class AndroidScheduleWorkProfileReconciler(
    private val controller: ScheduleWorkProfileController,
) : ScheduleWorkProfileReconciler {
    constructor(
        workProfileRepository: WorkProfileRepository,
        quietModeController: QuietModeController,
        actionDispatcher: WorkProfileActionDispatcher,
    ) : this(
        controller = AndroidScheduleWorkProfileController(
            workProfileRepository = workProfileRepository,
            quietModeController = quietModeController,
            actionDispatcher = actionDispatcher,
        ),
    )

    override fun reconcile(expectedState: WorkProfileScheduleExpectedState): ScheduleWorkProfileReconciliation {
        val selectedProfile = when (controller.resolveSelectedProfile()) {
            ScheduleWorkProfileResolution.Unavailable -> return blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.UNAVAILABLE,
                failureCategory = ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE,
            )
            ScheduleWorkProfileResolution.Missing -> return blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            )
            is ScheduleWorkProfileResolution.Selected -> controller.resolveSelectedProfile()
        }

        val selectedHandle = (selectedProfile as ScheduleWorkProfileResolution.Selected).handle
        val expectedQuietMode = expectedState.toQuietModeEnabled()
        val currentQuietMode = controller.isQuietModeEnabled(selectedHandle)
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
        return when (val actionResult = controller.dispatch(selectedHandle, quietModeAction)) {
            ScheduleWorkProfileDispatchResult.Ignored -> failedAfterActionRequest(
                requestedAction = requestedAction,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            )
            ScheduleWorkProfileDispatchResult.MissingProfile -> blocked(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            )
            is ScheduleWorkProfileDispatchResult.Failed -> failedAfterActionRequest(
                requestedAction = requestedAction,
                failureCategory = classifyQuietModeRequestFailure(actionResult.error),
            )
            ScheduleWorkProfileDispatchResult.Completed -> confirmFinalState(
                handle = selectedHandle,
                expectedQuietMode = expectedQuietMode,
                requestedAction = requestedAction,
                quietModeAction = quietModeAction,
            )
        }
    }

    private fun confirmFinalState(
        handle: ScheduleWorkProfileHandle,
        expectedQuietMode: Boolean,
        requestedAction: ScheduleRuntimeRequestedAction,
        quietModeAction: QuietModeAction,
    ): ScheduleWorkProfileReconciliation {
        val finalQuietMode = controller.isQuietModeEnabled(handle)
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
