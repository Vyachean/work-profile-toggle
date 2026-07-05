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
                } catch (_: Exception) {
                    // Do not let receiver background work crash the process.
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

internal sealed class ScheduleBoundaryDispatchResult {
    object Dispatched : ScheduleBoundaryDispatchResult()
    object FailedToDispatch : ScheduleBoundaryDispatchResult()
}
