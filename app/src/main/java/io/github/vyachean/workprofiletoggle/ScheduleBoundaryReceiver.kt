package io.github.vyachean.workprofiletoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executors

internal const val ACTION_SCHEDULE_BOUNDARY = "io.github.vyachean.workprofiletoggle.action.SCHEDULE_BOUNDARY"

class ScheduleBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCHEDULE_BOUNDARY) return

        val pendingResult = AndroidScheduleBoundaryPendingResult(goAsync())
        val handler = WorkProfileAppDependencies(context.applicationContext).scheduleBoundaryHandler
        runner.dispatch(pendingResult = pendingResult, handler = handler)
    }

    private class AndroidScheduleBoundaryPendingResult(
        private val pendingResult: PendingResult,
    ) : ScheduleBoundaryPendingResult {
        override fun finish() {
            pendingResult.finish()
        }
    }

    companion object {
        private val runner = ScheduleBoundaryAsyncRunner(
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "schedule-boundary-receiver")
            },
        )
    }
}
