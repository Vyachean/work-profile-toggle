package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.ZonedDateTime

internal class ScheduleAlarmScheduler(
    private val backend: ScheduleAlarmBackend,
    private val clock: Clock,
) {
    fun schedule(request: ScheduleAlarmRequest): ScheduleAlarmScheduleResult {
        if (!request.triggerAt.isAfter(ZonedDateTime.now(clock))) {
            return ScheduleAlarmScheduleResult.Blocked(
                ScheduleAlarmBlockedReason.BOUNDARY_NOT_IN_FUTURE,
            )
        }
        if (request.precision == ScheduleAlarmPrecision.EXACT && !backend.canScheduleExactAlarms()) {
            return ScheduleAlarmScheduleResult.Blocked(
                ScheduleAlarmBlockedReason.EXACT_ALARM_ACCESS_MISSING,
            )
        }

        return try {
            backend.cancel()
            when (request.precision) {
                ScheduleAlarmPrecision.INEXACT -> backend.setInexact(request.triggerAt.toInstant().toEpochMilli())
                ScheduleAlarmPrecision.EXACT -> backend.setExact(request.triggerAt.toInstant().toEpochMilli())
            }
            ScheduleAlarmScheduleResult.Scheduled(request)
        } catch (exception: SecurityException) {
            mapSecurityException(request)
        } catch (exception: RuntimeException) {
            ScheduleAlarmScheduleResult.Failed(ScheduleAlarmFailureReason.ANDROID_ALARM_REJECTED)
        }
    }

    fun cancel(): ScheduleAlarmCancelResult {
        return try {
            backend.cancel()
            ScheduleAlarmCancelResult.Cancelled
        } catch (exception: RuntimeException) {
            ScheduleAlarmCancelResult.Failed(ScheduleAlarmFailureReason.ANDROID_ALARM_REJECTED)
        }
    }

    private fun mapSecurityException(request: ScheduleAlarmRequest): ScheduleAlarmScheduleResult {
        return if (request.precision == ScheduleAlarmPrecision.EXACT) {
            ScheduleAlarmScheduleResult.Blocked(ScheduleAlarmBlockedReason.EXACT_ALARM_ACCESS_MISSING)
        } else {
            ScheduleAlarmScheduleResult.Failed(ScheduleAlarmFailureReason.ANDROID_ALARM_REJECTED)
        }
    }
}

internal data class ScheduleAlarmRequest(
    val triggerAt: ZonedDateTime,
    val precision: ScheduleAlarmPrecision,
)

internal enum class ScheduleAlarmPrecision {
    INEXACT,
    EXACT,
}

internal sealed class ScheduleAlarmScheduleResult {
    data class Scheduled(
        val request: ScheduleAlarmRequest,
    ) : ScheduleAlarmScheduleResult()

    data class Blocked(
        val reason: ScheduleAlarmBlockedReason,
    ) : ScheduleAlarmScheduleResult()

    data class Failed(
        val reason: ScheduleAlarmFailureReason,
    ) : ScheduleAlarmScheduleResult()
}

internal sealed class ScheduleAlarmCancelResult {
    data object Cancelled : ScheduleAlarmCancelResult()

    data class Failed(
        val reason: ScheduleAlarmFailureReason,
    ) : ScheduleAlarmCancelResult()
}

internal enum class ScheduleAlarmBlockedReason {
    BOUNDARY_NOT_IN_FUTURE,
    EXACT_ALARM_ACCESS_MISSING,
}

internal enum class ScheduleAlarmFailureReason {
    ANDROID_ALARM_REJECTED,
}

internal interface ScheduleAlarmBackend {
    fun canScheduleExactAlarms(): Boolean
    fun setInexact(triggerAtEpochMillis: Long)
    fun setExact(triggerAtEpochMillis: Long)
    fun cancel()
}
