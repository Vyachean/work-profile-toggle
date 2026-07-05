package io.github.vyachean.workprofiletoggle

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

internal class AndroidScheduleAlarmBackend(
    private val alarmManager: AlarmManager,
    private val operation: PendingIntent,
) : ScheduleAlarmBackend {
    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun setInexact(triggerAtEpochMillis: Long) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
    }

    override fun setExact(triggerAtEpochMillis: Long) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
    }

    override fun cancel() {
        alarmManager.cancel(operation)
    }
}
