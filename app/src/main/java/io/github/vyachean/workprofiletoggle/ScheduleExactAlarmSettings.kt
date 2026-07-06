package io.github.vyachean.workprofiletoggle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

private val exactAlarmSettingsAction = listOf(
    "android",
    "settings",
    "REQUEST_SCHEDULE_EXACT_ALARM",
).joinToString(".")

internal object ScheduleExactAlarmSettings {
    fun open(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

        return try {
            context.startActivity(
                Intent(exactAlarmSettingsAction).apply {
                    data = Uri.parse("package:${context.packageName}")
                },
            )
            true
        } catch (exception: RuntimeException) {
            false
        }
    }
}
