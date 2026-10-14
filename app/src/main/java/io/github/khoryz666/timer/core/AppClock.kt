package io.github.khoryz666.timer.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A half-open span of epoch milliseconds: [start, end). */
data class EpochRange(val start: Long, val end: Long)

/** Wall-clock time and the device's time zone, injectable so time math can be tested. */
interface AppClock {
    fun now(): Long
    fun zone(): ZoneId

    fun today(): LocalDate = dateOf(now())
    fun dateOf(epochMs: Long): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone()).toLocalDate()

    /** From the start of [date] to the start of the next day. */
    fun dayBounds(date: LocalDate): EpochRange = bounds(date, date.plusDays(1))

    /** From the start of [from] to the start of [until]. */
    fun bounds(from: LocalDate, until: LocalDate): EpochRange =
        EpochRange(startOf(from), startOf(until))

    private fun startOf(date: LocalDate): Long = date.atStartOfDay(zone()).toInstant().toEpochMilli()

    /** Emits [now] immediately, then every [periodMs]. */
    fun ticks(periodMs: Long): Flow<Long> = flow {
        while (true) {
            emit(now())
            delay(periodMs)
        }
    }

    object System : AppClock {
        override fun now(): Long = java.lang.System.currentTimeMillis()
        override fun zone(): ZoneId = ZoneId.systemDefault()
    }
}
