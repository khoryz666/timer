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

    private fun session(pillar: Pillar, from: LocalDateTime, to: LocalDateTime?, projectId: Long? = null) =
        Session(pillar = pillar, projectId = projectId, startedAt = clock.at(from), endedAt = to?.let(clock::at))

    private fun project(projectId: Long, from: LocalDateTime, to: LocalDateTime?) = session(Pillar.PROJECT, from, to, projectId)

    @Test
    fun `a session crossing midnight counts entirely toward the day it ends`() {
        val session = project(1, LocalDateTime.of(2026, 10, 13, 23, 0), LocalDateTime.of(2026, 10, 14, 0, 30))

        val totals = dailyTotals(listOf(session), clock.now(), clock)

        assertNull(totals[oct13])
        assertEquals(90 * minute, totals.getValue(oct14).ms(Pillar.PROJECT))
    }

    @Test
    fun `the running session counts toward today, up to now`() {
        val running = session(Pillar.JOY, LocalDateTime.of(2026, 10, 13, 23, 30), null)

        val totals = dailyTotals(listOf(running), clock.now(), clock)

        assertEquals(mapOf(oct14 to DayTotals(byPillar = mapOf(Pillar.JOY to 90 * minute))), totals)
    }

    @Test
    fun `time is kept per track, and Project time per project`() {
        val sessions = listOf(
            project(1, LocalDateTime.of(2026, 10, 13, 9, 0), LocalDateTime.of(2026, 10, 13, 10, 30)),
            project(2, LocalDateTime.of(2026, 10, 13, 11, 0), LocalDateTime.of(2026, 10, 13, 11, 45)),
            project(1, LocalDateTime.of(2026, 10, 13, 14, 0), LocalDateTime.of(2026, 10, 13, 14, 30)),
            session(Pillar.NEETCODE, LocalDateTime.of(2026, 10, 13, 7, 30), LocalDateTime.of(2026, 10, 13, 8, 30)),
            session(Pillar.READING, LocalDateTime.of(2026, 10, 13, 15, 0), LocalDateTime.of(2026, 10, 13, 16, 40)),
            session(Pillar.JOY, LocalDateTime.of(2026, 10, 13, 19, 0), LocalDateTime.of(2026, 10, 13, 21, 0)),
        )

        val day = dailyTotals(sessions, clock.now(), clock).getValue(oct13)

        assertEquals(165 * minute, day.ms(Pillar.PROJECT))
        assertEquals(mapOf(1L to 120 * minute, 2L to 45 * minute), day.byProject)
        assertEquals(60 * minute, day.ms(Pillar.NEETCODE))
        assertEquals(100 * minute, day.ms(Pillar.READING))
        assertEquals(325 * minute, day.focusMs)
        assertEquals(120 * minute, day.joyMs)
    }

    @Test
    fun `focus blocks are whole blocks per track - minutes don't pool across tracks`() {
        val day = DayTotals(byPillar = mapOf(Pillar.PROJECT to 150 * minute, Pillar.NEETCODE to 40 * minute, Pillar.READING to 30 * minute))
        assertEquals(2, day.focusBlocks(60 * minute)) // not 220 / 60 = 3
    }

    @Test
    fun `a session whose end is before its start (clock set back) counts as zero`() {
        val session = Session(pillar = Pillar.JOY, projectId = null, startedAt = clock.now(), endedAt = clock.now() - 10 * minute)
        assertEquals(0L, session.durationMs(clock.now()))
    }
}
