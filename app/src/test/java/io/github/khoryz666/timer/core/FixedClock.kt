package io.github.khoryz666.timer.core

import java.time.LocalDateTime
import java.time.ZoneId

/** A settable clock for tests. Defaults to the developer's zone (UTC+8, no DST). */
class FixedClock(start: LocalDateTime, private val zone: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")) : AppClock {
    var nowMs: Long = at(start)

    override fun now() = nowMs
    override fun zone() = zone

    fun set(dateTime: LocalDateTime) {
        nowMs = at(dateTime)
    }

    fun at(dateTime: LocalDateTime): Long = dateTime.atZone(zone).toInstant().toEpochMilli()
}
