package io.github.vyachean.workprofiletoggle

import android.app.AlarmManager
import android.content.Context
import android.os.Build

internal class AndroidScheduleExactAlarmAccess(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    fun state(): ScheduleExactAlarmAccessState {
        return resolveScheduleExactAlarmAccess(
            sdkInt = Build.VERSION.SDK_INT,
            exactAlarmAccessIntroducedSdkInt = Build.VERSION_CODES.S,
            canScheduleExactAlarms = {
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            },
        )
    }
}
