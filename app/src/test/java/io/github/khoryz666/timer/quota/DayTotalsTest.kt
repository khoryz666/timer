package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.FixedClock
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DayTotalsTest {
    private val minute = 60_000L
    private val clock = FixedClock(LocalDateTime.of(2026, 10, 14, 1, 0))
    private val oct13 = LocalDate.of(2026, 10, 13)
    private val oct14 = LocalDate.of(2026, 10, 14)

    private fun engine(projectId: Long, from: LocalDateTime, to: LocalDateTime?) =
        Session(pillar = Pillar.ENGINE, projectId = projectId, startedAt = clock.at(from), endedAt = to?.let(clock::at))

    private fun joy(from: LocalDateTime, to: LocalDateTime?) =
        Session(pillar = Pillar.JOY, projectId = null, startedAt = clock.at(from), endedAt = to?.let(clock::at))

    @Test
    fun `a session crossing midnight counts entirely toward the day it ends`() {
        val session = engine(1, LocalDateTime.of(2026, 10, 13, 23, 0), LocalDateTime.of(2026, 10, 14, 0, 30))

        val totals = dailyTotals(listOf(session), clock.now(), clock)

        assertNull(totals[oct13])
        assertEquals(90 * minute, totals.getValue(oct14).engineMs)
    }

    @Test
    fun `the running session counts toward today, up to now`() {
        val running = joy(LocalDateTime.of(2026, 10, 13, 23, 30), null)

        val totals = dailyTotals(listOf(running), clock.now(), clock)

        assertEquals(mapOf(oct14 to DayTotals(joyMs = 90 * minute)), totals)
    }

    @Test
    fun `engine time is split by project and joy is kept apart`() {
        val sessions = listOf(
            engine(1, LocalDateTime.of(2026, 10, 13, 9, 0), LocalDateTime.of(2026, 10, 13, 10, 30)),
            engine(2, LocalDateTime.of(2026, 10, 13, 11, 0), LocalDateTime.of(2026, 10, 13, 11, 45)),
            engine(1, LocalDateTime.of(2026, 10, 13, 14, 0), LocalDateTime.of(2026, 10, 13, 14, 30)),
            joy(LocalDateTime.of(2026, 10, 13, 19, 0), LocalDateTime.of(2026, 10, 13, 21, 0)),
        )

        val day = dailyTotals(sessions, clock.now(), clock).getValue(oct13)

        assertEquals(165 * minute, day.engineMs)
        assertEquals(mapOf(1L to 120 * minute, 2L to 45 * minute), day.engineByProject)
        assertEquals(120 * minute, day.joyMs)
    }

    @Test
    fun `a session whose end is before its start (clock set back) counts as zero`() {
        val session = Session(pillar = Pillar.JOY, projectId = null, startedAt = clock.now(), endedAt = clock.now() - 10 * minute)
        assertEquals(0L, session.durationMs(clock.now()))
    }
}
