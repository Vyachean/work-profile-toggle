package io.github.vyachean.workprofiletoggle

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val SCHEDULE_BOUNDARY_REQUEST_CODE = 1001

internal class AndroidScheduleBoundaryPendingIntentFactory(
    private val context: Context,
) {
    fun create(): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            SCHEDULE_BOUNDARY_REQUEST_CODE,
            Intent(context, ScheduleBoundaryReceiver::class.java).setAction(ACTION_SCHEDULE_BOUNDARY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
