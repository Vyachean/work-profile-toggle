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
                } catch (exception: Exception) {
                    reportFailure(handler, exception)
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

    private fun reportFailure(
        handler: ScheduleBoundaryHandler,
        exception: Exception,
    ) {
        try {
            handler.handleFailure(exception)
        } catch (_: Exception) {
            // Do not let failure reporting crash receiver background work.
        }
    }
}

internal interface ScheduleBoundaryPendingResult {
    fun finish()
}

internal interface ScheduleBoundaryHandler {
    fun handleBoundary()

    fun handleFailure(exception: Exception) = Unit
}

internal sealed class ScheduleBoundaryDispatchResult {
    object Dispatched : ScheduleBoundaryDispatchResult()
    object FailedToDispatch : ScheduleBoundaryDispatchResult()
}
