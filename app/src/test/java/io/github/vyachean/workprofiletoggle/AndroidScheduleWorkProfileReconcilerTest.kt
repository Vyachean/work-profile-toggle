package io.github.vyachean.workprofiletoggle

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidScheduleWorkProfileReconcilerTest {
    @Test
    fun blocksWhenSelectedProfileIsUnavailable() {
        val fixture = fixture(
            resolution = ScheduleWorkProfileResolution.Unavailable,
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.UNAVAILABLE,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                failureCategory = ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE,
            ),
            result,
        )
        assertEquals(1, fixture.controller.resolveCalls)
        assertEquals(emptyList<ScheduleWorkProfileHandle>(), fixture.controller.quietModeReads)
        assertEquals(emptyList<QuietModeAction>(), fixture.controller.dispatchedActions)
    }

    @Test
    fun blocksWhenSelectedProfileIsMissing() {
        val fixture = fixture(
            resolution = ScheduleWorkProfileResolution.Missing,
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            ),
            result,
        )
        assertEquals(1, fixture.controller.resolveCalls)
        assertEquals(emptyList<ScheduleWorkProfileHandle>(), fixture.controller.quietModeReads)
        assertEquals(emptyList<QuietModeAction>(), fixture.controller.dispatchedActions)
    }

    @Test
    fun succeedsWithoutDispatchWhenCurrentQuietModeAlreadyMatchesExpectedState() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false)),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
            ),
            result,
        )
        assertEquals(listOf(selectedProfileHandle), fixture.controller.quietModeReads)
        assertEquals(emptyList<QuietModeAction>(), fixture.controller.dispatchedActions)
    }

    @Test
    fun dispatchesPauseAndConfirmsFinalPausedState() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false), Result.success(true)),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
        assertEquals(listOf(selectedProfileHandle, selectedProfileHandle), fixture.controller.quietModeReads)
    }

    @Test
    fun dispatchesResumeAndConfirmsFinalActiveState() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(true), Result.success(false)),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = true,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Disable), fixture.controller.dispatchedActions)
        assertEquals(listOf(selectedProfileHandle, selectedProfileHandle), fixture.controller.quietModeReads)
    }

    @Test
    fun reportsPermissionMissingWhenInitialQuietModeReadThrowsSecurityException() {
        val fixture = fixture(
            quietModeResults = listOf(Result.failure(SecurityException("Missing permission"))),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
            ),
            result,
        )
        assertEquals(emptyList<QuietModeAction>(), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsWorkProfileUnavailableWhenInitialQuietModeReadFailsForOtherReason() {
        val fixture = fixture(
            quietModeResults = listOf(Result.failure(IllegalStateException("Profile unavailable"))),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.WORK_PROFILE_UNAVAILABLE,
            ),
            result,
        )
        assertEquals(emptyList<QuietModeAction>(), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsAndroidRejectedWhenDispatchIsIgnored() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false)),
            dispatchResult = ScheduleWorkProfileDispatchResult.Ignored,
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsPermissionMissingWhenDispatchFailsWithSecurityException() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(true)),
            dispatchResult = ScheduleWorkProfileDispatchResult.Failed(SecurityException("Missing permission")),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Disable), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsAndroidRejectedWhenDispatchFailsForOtherReason() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false)),
            dispatchResult = ScheduleWorkProfileDispatchResult.Failed(IllegalStateException("Rejected")),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
    }

    @Test
    fun blocksWhenDispatchReportsMissingProfile() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false)),
            dispatchResult = ScheduleWorkProfileDispatchResult.MissingProfile,
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                selectedProfileStatus = ScheduleRuntimeProfileStatus.MISSING,
                actionResult = ScheduleRuntimeActionResult.BLOCKED,
                failureCategory = ScheduleRuntimeFailureCategory.SELECTED_PROFILE_MISSING,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
    }

    @Test
    fun keepsActionSuccessWhenFinalQuietModeReadThrowsSecurityException() {
        val fixture = fixture(
            quietModeResults = listOf(
                Result.success(false),
                Result.failure(SecurityException("Missing permission")),
            ),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.SUCCEEDED,
                finalStateConfirmed = false,
                failureCategory = ScheduleRuntimeFailureCategory.PERMISSION_MISSING,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsCredentialRequiredWhenResumeIsNotConfirmed() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(true), Result.success(true)),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.ACTIVE)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.ACTIVATE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.CREDENTIAL_REQUIRED,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Disable), fixture.controller.dispatchedActions)
    }

    @Test
    fun reportsAndroidRejectedWhenPauseIsNotConfirmed() {
        val fixture = fixture(
            quietModeResults = listOf(Result.success(false), Result.success(false)),
        )

        val result = fixture.reconciler.reconcile(WorkProfileScheduleExpectedState.PAUSED)

        assertEquals(
            reconciliation(
                requestedAction = ScheduleRuntimeRequestedAction.PAUSE_WORK_PROFILE,
                actionResult = ScheduleRuntimeActionResult.FAILED,
                failureCategory = ScheduleRuntimeFailureCategory.ANDROID_REQUEST_REJECTED,
            ),
            result,
        )
        assertEquals(listOf(QuietModeAction.Enable), fixture.controller.dispatchedActions)
    }

    private fun fixture(
        resolution: ScheduleWorkProfileResolution = ScheduleWorkProfileResolution.Selected(selectedProfileHandle),
        quietModeResults: List<Result<Boolean>> = emptyList(),
        dispatchResult: ScheduleWorkProfileDispatchResult = ScheduleWorkProfileDispatchResult.Completed,
    ): Fixture {
        val controller = FakeScheduleWorkProfileController(
            resolution = resolution,
            quietModeResults = quietModeResults,
            dispatchResult = dispatchResult,
        )
        return Fixture(
            controller = controller,
            reconciler = AndroidScheduleWorkProfileReconciler(controller),
        )
    }

    private fun reconciliation(
        selectedProfileStatus: ScheduleRuntimeProfileStatus = ScheduleRuntimeProfileStatus.SELECTED,
        requestedAction: ScheduleRuntimeRequestedAction = ScheduleRuntimeRequestedAction.NONE,
        actionResult: ScheduleRuntimeActionResult,
        finalStateConfirmed: Boolean = false,
        failureCategory: ScheduleRuntimeFailureCategory? = null,
    ): ScheduleWorkProfileReconciliation {
        return ScheduleWorkProfileReconciliation(
            selectedProfileStatus = selectedProfileStatus,
            requestedAction = requestedAction,
            actionResult = actionResult,
            finalStateConfirmed = finalStateConfirmed,
            failureCategory = failureCategory,
        )
    }

    private data class Fixture(
        val controller: FakeScheduleWorkProfileController,
        val reconciler: AndroidScheduleWorkProfileReconciler,
    )

    private class FakeScheduleWorkProfileController(
        private val resolution: ScheduleWorkProfileResolution,
        quietModeResults: List<Result<Boolean>>,
        private val dispatchResult: ScheduleWorkProfileDispatchResult,
    ) : ScheduleWorkProfileController {
        private val remainingQuietModeResults = quietModeResults.toMutableList()
        val quietModeReads = mutableListOf<ScheduleWorkProfileHandle>()
        val dispatchedActions = mutableListOf<QuietModeAction>()
        var resolveCalls = 0

        override fun resolveSelectedProfile(): ScheduleWorkProfileResolution {
            resolveCalls += 1
            return resolution
        }

        override fun isQuietModeEnabled(handle: ScheduleWorkProfileHandle): Result<Boolean> {
            quietModeReads += handle
            return remainingQuietModeResults.removeAt(0)
        }

        override fun dispatch(
            handle: ScheduleWorkProfileHandle,
            requestedAction: QuietModeAction,
        ): ScheduleWorkProfileDispatchResult {
            dispatchedActions += requestedAction
            return dispatchResult
        }
    }

    private companion object {
        val selectedProfileHandle = object : ScheduleWorkProfileHandle {}
    }
}
