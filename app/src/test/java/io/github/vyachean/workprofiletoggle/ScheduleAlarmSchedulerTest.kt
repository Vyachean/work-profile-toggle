package io.github.vyachean.workprofiletoggle

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleAlarmSchedulerTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 1, 5, 9, 0, 0, 0, zone)
    private val clock: Clock = Clock.fixed(now.toInstant(), zone)

    @Test
    fun schedulesInexactFutureAlarm() {
        val backend = FakeScheduleAlarmBackend()
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        val request = request(
            triggerAt = now.plusHours(1),
            precision = ScheduleAlarmPrecision.INEXACT,
        )

        val result = scheduler.schedule(request)

        assertEquals(ScheduleAlarmScheduleResult.Scheduled(request), result)
        assertEquals(
            listOf(
                BackendCall.Cancel,
                BackendCall.SetInexact(now.plusHours(1).toEpochMillis()),
            ),
            backend.calls,
        )
    }

    @Test
    fun schedulesExactFutureAlarmWhenExactAlarmAccessIsAvailable() {
        val backend = FakeScheduleAlarmBackend(canScheduleExactAlarms = true)
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        val request = request(
            triggerAt = now.plusHours(1),
            precision = ScheduleAlarmPrecision.EXACT,
        )

        val result = scheduler.schedule(request)

        assertEquals(ScheduleAlarmScheduleResult.Scheduled(request), result)
        assertEquals(
            listOf(
                BackendCall.Cancel,
                BackendCall.SetExact(now.plusHours(1).toEpochMillis()),
            ),
            backend.calls,
        )
    }

    @Test
    fun blocksExactAlarmWhenExactAlarmAccessIsMissing() {
        val backend = FakeScheduleAlarmBackend(canScheduleExactAlarms = false)
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        val request = request(
            triggerAt = now.plusHours(1),
            precision = ScheduleAlarmPrecision.EXACT,
        )

        val result = scheduler.schedule(request)

        assertEquals(
            ScheduleAlarmScheduleResult.Blocked(ScheduleAlarmBlockedReason.EXACT_ALARM_ACCESS_MISSING),
            result,
        )
        assertEquals(emptyList<BackendCall>(), backend.calls)
    }

    @Test
    fun blocksAlarmWhenBoundaryIsNow() {
        val backend = FakeScheduleAlarmBackend()
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        val request = request(
            triggerAt = now,
            precision = ScheduleAlarmPrecision.INEXACT,
        )

        val result = scheduler.schedule(request)

        assertEquals(
            ScheduleAlarmScheduleResult.Blocked(ScheduleAlarmBlockedReason.BOUNDARY_NOT_IN_FUTURE),
            result,
        )
        assertEquals(emptyList<BackendCall>(), backend.calls)
    }

    @Test
    fun blocksAlarmWhenBoundaryIsInPast() {
        val backend = FakeScheduleAlarmBackend()
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)
        val request = request(
            triggerAt = now.minusMinutes(1),
            precision = ScheduleAlarmPrecision.INEXACT,
        )

        val result = scheduler.schedule(request)

        assertEquals(
            ScheduleAlarmScheduleResult.Blocked(ScheduleAlarmBlockedReason.BOUNDARY_NOT_IN_FUTURE),
            result,
        )
        assertEquals(emptyList<BackendCall>(), backend.calls)
    }

    @Test
    fun cancelDelegatesToBackend() {
        val backend = FakeScheduleAlarmBackend()
        val scheduler = ScheduleAlarmScheduler(backend = backend, clock = clock)

        scheduler.cancel()

        assertEquals(listOf(BackendCall.Cancel), backend.calls)
    }

    private fun request(
        triggerAt: ZonedDateTime,
        precision: ScheduleAlarmPrecision,
    ): ScheduleAlarmRequest {
        return ScheduleAlarmRequest(
            triggerAt = triggerAt,
            precision = precision,
        )
    }

    private fun ZonedDateTime.toEpochMillis(): Long {
        return toInstant().toEpochMilli()
    }

    private class FakeScheduleAlarmBackend(
        private val canScheduleExactAlarms: Boolean = true,
    ) : ScheduleAlarmBackend {
        val calls = mutableListOf<BackendCall>()

        override fun canScheduleExactAlarms(): Boolean {
            return canScheduleExactAlarms
        }

        override fun setInexact(triggerAtEpochMillis: Long) {
            calls.add(BackendCall.SetInexact(triggerAtEpochMillis))
        }

        override fun setExact(triggerAtEpochMillis: Long) {
            calls.add(BackendCall.SetExact(triggerAtEpochMillis))
        }

        override fun cancel() {
            calls.add(BackendCall.Cancel)
        }
    }

    private sealed class BackendCall {
        data object Cancel : BackendCall()
        data class SetInexact(val triggerAtEpochMillis: Long) : BackendCall()
        data class SetExact(val triggerAtEpochMillis: Long) : BackendCall()
    }
}
