package io.github.vyachean.workprofiletoggle

import java.util.concurrent.Executor

internal class ScheduleBoundaryAsyncRunner(
    private val executor: Executor,
) {
    fun dispatch(
        pendingResult: ScheduleBoundaryPendingResult,
        handler: ScheduleBoundaryHandler,
    ): ScheduleBoundaryDispatchResult {
        return try {
            executor.execute {
                try {
                    handler.handleBoundary()
                } catch (_: RuntimeException) {
                    // The receiver boundary must not crash the process. Runtime failures are
                    // handled by the runtime layer in later reconciliation work.
                } finally {
                    pendingResult.finish()
                }
            }
            ScheduleBoundaryDispatchResult.Dispatched
        } catch (_: RuntimeException) {
            pendingResult.finish()
            ScheduleBoundaryDispatchResult.FailedToDispatch
        }
    }
}

internal interface ScheduleBoundaryPendingResult {
    fun finish()
}

internal interface ScheduleBoundaryHandler {
    fun handleBoundary()
}

internal enum class ScheduleBoundaryDispatchResult {
    DISPATCHED,
    FAILED_TO_DISPATCH,
    ;

    companion object {
        val Dispatched: ScheduleBoundaryDispatchResult = DISPATCHED
        val FailedToDispatch: ScheduleBoundaryDispatchResult = FAILED_TO_DISPATCH
    }
}
