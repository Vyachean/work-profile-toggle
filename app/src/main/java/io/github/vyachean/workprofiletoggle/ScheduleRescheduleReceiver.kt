package io.github.vyachean.workprofiletoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executors

internal const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_CHANGED =
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

internal val SCHEDULE_RESCHEDULE_ACTIONS: Set<String> = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_CHANGED,
)

internal fun isScheduleRescheduleAction(action: String?): Boolean {
    return action in SCHEDULE_RESCHEDULE_ACTIONS
}

class ScheduleRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isScheduleRescheduleAction(intent.action)) return

        val asyncResult = goAsync()
        if (asyncResult == null) return

        val handler = ScheduleRescheduleHandler(
            scheduleBoundaryPlanner = WorkProfileAppDependencies(
                context.applicationContext,
            ).scheduleBoundaryPlanner,
        )
        runner.dispatch(
            pendingResult = AndroidScheduleReschedulePendingResult(asyncResult),
            handler = handler,
        )
    }

    private class AndroidScheduleReschedulePendingResult(
        private val pendingResult: BroadcastReceiver.PendingResult,
    ) : ScheduleBoundaryPendingResult {
        override fun finish() {
            pendingResult.finish()
        }
    }

    companion object {
        private val runner = ScheduleBoundaryAsyncRunner(
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "schedule-reschedule-receiver")
            },
        )
    }
}

private class ScheduleRescheduleHandler(
    private val scheduleBoundaryPlanner: ScheduleBoundaryPlanner,
) : ScheduleBoundaryHandler {
    override fun handleBoundary() {
        scheduleBoundaryPlanner.refresh()
    }
}
