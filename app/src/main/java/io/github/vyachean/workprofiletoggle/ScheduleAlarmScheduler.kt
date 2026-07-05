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

        return runCatching {
            backend.cancel()
            when (request.precision) {
                ScheduleAlarmPrecision.INEXACT -> backend.setInexact(request.triggerAt.toInstant().toEpochMilli())
                ScheduleAlarmPrecision.EXACT -> backend.setExact(request.triggerAt.toInstant().toEpochMilli())
            }
        }.fold(
            onSuccess = { ScheduleAlarmScheduleResult.Scheduled(request) },
            onFailure = { throwable -> mapScheduleFailure(request, throwable) },
        )
    }

    fun cancel(): ScheduleAlarmCancelResult {
        return runCatching { backend.cancel() }.fold(
            onSuccess = { ScheduleAlarmCancelResult.Cancelled },
            onFailure = { ScheduleAlarmCancelResult.Failed(ScheduleAlarmFailureReason.ANDROID_ALARM_REJECTED) },
        )
    }

    private fun mapScheduleFailure(
        request: ScheduleAlarmRequest,
        throwable: Throwable,
    ): ScheduleAlarmScheduleResult {
        return if (request.precision == ScheduleAlarmPrecision.EXACT && throwable is SecurityException) {
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
