package io.github.vyachean.workprofiletoggle

import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun finishesPendingResultWhenHandlerFails() {
        val executor = RecordingExecutor()
        val pendingResult = RecordingPendingResult()
        val handler = FailingHandler()
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

    private class FailingHandler : ScheduleBoundaryHandler {
        override fun handleBoundary() {
            throw RuntimeException("Handler failed")
        }
    }
}
