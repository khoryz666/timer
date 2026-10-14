package io.github.khoryz666.timer.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DurationsTest {
    private val minute = 60_000L

    @Test
    fun `formatDuration shows hours and minutes, rounding down`() {
        assertEquals("0m", formatDuration(59_999))
        assertEquals("45m", formatDuration(45 * minute))
        assertEquals("3h", formatDuration(180 * minute))
        assertEquals("3h 12m", formatDuration(192 * minute + 59_000))
        assertEquals("26h 5m", formatDuration((26 * 60 + 5) * minute))
    }

    @Test
    fun `formatDuration treats negative durations as zero`() {
        assertEquals("0m", formatDuration(-5 * minute))
    }

    @Test
    fun `formatElapsed shows a stopwatch`() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("4:05", formatElapsed(245_000))
        assertEquals("1:02:03", formatElapsed(3_723_000))
    }

    @Test
    fun `formatPercent rounds to whole percent`() {
        assertEquals("58%", formatPercent(0.5849))
        assertEquals("0%", formatPercent(0.0))
        assertEquals("100%", formatPercent(1.0))
    }

    @Test
    fun `dayBounds spans a 23-hour day across a DST change`() {
        val clock = FixedClock(LocalDateTime.of(2026, 3, 29, 12, 0), ZoneId.of("Europe/Berlin"))
        val day = clock.dayBounds(LocalDate.of(2026, 3, 29))
        assertEquals(23 * 60 * minute, day.end - day.start)
    }
}
