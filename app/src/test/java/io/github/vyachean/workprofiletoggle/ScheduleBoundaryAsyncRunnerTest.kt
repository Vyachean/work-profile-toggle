package io.github.vyachean.workprofiletoggle

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleBoundaryAsyncRunnerTest {
    @Test
    fun dispatchesHandlerWithoutFinishingBeforeBackgroundCommandRuns() {
        val executor = RecordingExecutor()
        val pendingResult = RecordingPendingResult()
        val handler = RecordingHandler()
        val runner = ScheduleBoundaryAsyncRunner(executor)

        val result = runner.dispatch(pendingResult = pendingResult, handler = handler)

        assertEquals(ScheduleBoundaryDispatchResult.Dispatched, result)
        assertFalse(handler.handled)
        assertEquals(0, pendingResult.finishCount)

        executor.runCommand()

        assertTrue(handler.handled)
        assertEquals(1, pendingResult.finishCount)
    }

    @Test
    fun reportsFailureAndFinishesPendingResultWhenHandlerFailsWithException() {
        val executor = RecordingExecutor()
        val pendingResult = RecordingPendingResult()
        val failure = Exception("Handler failed")
        val handler = FailingHandler(failure)
        val runner = ScheduleBoundaryAsyncRunner(executor)

        val result = runner.dispatch(pendingResult = pendingResult, handler = handler)

        assertEquals(ScheduleBoundaryDispatchResult.Dispatched, result)

        executor.runCommand()

        assertSame(failure, handler.reportedFailure)
        assertEquals(1, pendingResult.finishCount)
    }

    @Test
    fun finishesPendingResultWhenFailureReportingFails() {
        val executor = RecordingExecutor()
        val pendingResult = RecordingPendingResult()
        val handler = FailingFailureReporter()
        val runner = ScheduleBoundaryAsyncRunner(executor)

        val result = runner.dispatch(pendingResult = pendingResult, handler = handler)

        assertEquals(ScheduleBoundaryDispatchResult.Dispatched, result)

        executor.runCommand()

        assertEquals(1, pendingResult.finishCount)
    }

    @Test
    fun finishesPendingResultWhenExecutorRejectsDispatch() {
        val pendingResult = RecordingPendingResult()
        val handler = RecordingHandler()
        val runner = ScheduleBoundaryAsyncRunner(RejectingExecutor())

        val result = runner.dispatch(pendingResult = pendingResult, handler = handler)

        assertEquals(ScheduleBoundaryDispatchResult.FailedToDispatch, result)
        assertFalse(handler.handled)
        assertEquals(1, pendingResult.finishCount)
    }

    private class RecordingExecutor : Executor {
        private var command: Runnable? = null

        override fun execute(command: Runnable) {
            check(this.command == null) { "command already recorded" }
            this.command = command
        }

        fun runCommand() {
            requireNotNull(command).run()
        }
    }

    private class RejectingExecutor : Executor {
        override fun execute(command: Runnable) {
            throw RuntimeException("Executor rejected command")
        }
    }

    private class RecordingPendingResult : ScheduleBoundaryPendingResult {
        var finishCount: Int = 0
            private set

        override fun finish() {
            finishCount += 1
        }
    }

    private class RecordingHandler : ScheduleBoundaryHandler {
        var handled: Boolean = false
            private set

        override fun handleBoundary() {
            handled = true
        }
    }

    private class FailingHandler(
        private val failure: Exception,
    ) : ScheduleBoundaryHandler {
        var reportedFailure: Exception? = null
            private set

        override fun handleBoundary() {
            throw failure
        }

        override fun handleFailure(exception: Exception) {
            reportedFailure = exception
        }
    }

    private class FailingFailureReporter : ScheduleBoundaryHandler {
        override fun handleBoundary() {
            throw Exception("Handler failed")
        }

        override fun handleFailure(exception: Exception) {
            throw Exception("Failure reporting failed")
        }
    }
}
