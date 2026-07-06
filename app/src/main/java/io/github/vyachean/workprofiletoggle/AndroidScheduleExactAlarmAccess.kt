package io.github.vyachean.workprofiletoggle

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

private val scheduleAlarmSettingsAction = listOf(
    "android.settings",
    "REQUEST_SCHEDULE_EXACT_ALARM",
).joinToString(".")

internal class AndroidScheduleExactAlarmAccess(
    private val context: Context,
) {
    private val appContext: Context = context.applicationContext

    fun state(): ScheduleExactAlarmAccessState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return ScheduleExactAlarmAccessState.NOT_REQUIRED
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (alarmManager.canScheduleExactAlarms()) {
            ScheduleExactAlarmAccessState.GRANTED
        } else {
            ScheduleExactAlarmAccessState.MISSING
        }
    }

    fun openAppSettings(): Boolean {
        return try {
            val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                scheduleAlarmSettingsAction
            } else {
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            }
            context.startActivity(
                Intent(action).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                },
            )
            true
        } catch (exception: RuntimeException) {
            false
        }
    }
}
