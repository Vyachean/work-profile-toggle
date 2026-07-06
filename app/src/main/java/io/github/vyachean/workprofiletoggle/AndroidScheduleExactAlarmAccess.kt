package io.github.vyachean.workprofiletoggle

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal class AndroidScheduleExactAlarmAccess(
    private val context: Context,
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

    fun openAppSettings(): Boolean {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                },
            )
            true
        } catch (exception: RuntimeException) {
            false
        }
    }
}
