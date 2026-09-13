package com.example.timer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `zero duration formats as all zeros`() {
        assertEquals("00 h 00 m 00 s", TimeFormatter.formatHMS(0L))
    }

    @Test
    fun `sub-minute duration formats seconds only`() {
        assertEquals("00 h 00 m 01 s", TimeFormatter.formatHMS(1_000L))
    }

    @Test
    fun `sub-hour duration formats minutes and seconds`() {
        assertEquals("00 h 01 m 01 s", TimeFormatter.formatHMS(61_000L))
    }

    @Test
    fun `duration over an hour formats hours minutes and seconds`() {
        assertEquals("01 h 01 m 01 s", TimeFormatter.formatHMS(3_661_000L))
    }

    @Test
    fun `hours are not capped at 24`() {
        val hundredHours = 100L * 60 * 60 * 1000
        assertEquals("100 h 00 m 00 s", TimeFormatter.formatHMS(hundredHours))
    }
}
