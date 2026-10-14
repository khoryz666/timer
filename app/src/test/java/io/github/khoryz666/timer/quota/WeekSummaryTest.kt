package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.FixedClock
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeekSummaryTest {
    private val minute = 60_000L
    private val clock = FixedClock(LocalDateTime.of(2026, 10, 18, 22, 0)) // Sunday evening
    private val monday = LocalDate.of(2026, 10, 12)
    private val kefos = Project(id = 1, name = "KEFOS", sharePercent = 60)
    private val qorFinder = Project(id = 2, name = "QorFinder", sharePercent = 40)

    private fun engine(projectId: Long, from: LocalDateTime, minutes: Long): Session {
        val start = clock.at(from)
        return Session(pillar = Pillar.ENGINE, projectId = projectId, startedAt = start, endedAt = start + minutes * minute)
    }

    private fun summary(sessions: List<Session>, projects: List<Project> = listOf(kefos, qorFinder)) =
        WeekSummary.of(monday, sessions, projects, QuotaSettings(), clock.now(), clock)

    @Test
    fun `weeks start on Monday`() {
        assertEquals(monday, LocalDate.of(2026, 10, 12).weekStart())
        assertEquals(monday, LocalDate.of(2026, 10, 15).weekStart())
        assertEquals(monday, LocalDate.of(2026, 10, 18).weekStart()) // Sunday belongs to the week before it
        assertEquals(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 19).weekStart())
    }

    @Test
    fun `days run Monday to Sunday`() {
        val days = summary(emptyList()).days.map { it.date }
        assertEquals((0L..6L).map { monday.plusDays(it) }, days)
    }

    @Test
    fun `weekly blocks are the sum of each day's whole blocks`() {
        val week = summary(
            listOf(
                engine(1, LocalDateTime.of(2026, 10, 12, 9, 0), 200), // Mon: 2 blocks + 20 min
                engine(2, LocalDateTime.of(2026, 10, 13, 9, 0), 80), //  Tue: 0 blocks
                engine(1, LocalDateTime.of(2026, 10, 14, 9, 0), 360), // Wed: 4 blocks
            )
        )

        assertEquals(listOf(2, 0, 4, 0, 0, 0, 0), week.days.map { it.engineBlocks })
        assertEquals(6, week.engineBlocks) // not (640 / 90) = 7: leftovers don't carry over
        assertEquals(640 * minute, week.engineMs)
        assertEquals(24, week.weeklyTargetBlocks)
    }

    @Test
    fun `sessions ending outside the week are ignored, including a Sunday-night session that ends on Monday`() {
        val week = summary(
            listOf(
                engine(1, LocalDateTime.of(2026, 10, 11, 23, 0), 120), // Sun -> Mon 01:00: counts Monday
                engine(1, LocalDateTime.of(2026, 10, 18, 23, 30), 60), //  Sun -> next Mon: next week
                engine(1, LocalDateTime.of(2026, 10, 5, 9, 0), 90), //    previous week
            )
        )

        assertEquals(120 * minute, week.days.first().totals.engineMs)
        assertEquals(120 * minute, week.engineMs)
    }

    @Test
    fun `project split compares actual share of engine time with the target share`() {
        val week = summary(
            listOf(
                engine(1, LocalDateTime.of(2026, 10, 12, 9, 0), 270),
                engine(2, LocalDateTime.of(2026, 10, 13, 9, 0), 90),
            )
        )

        val split = week.split.associateBy { it.project.name }
        assertEquals(0.75, split.getValue("KEFOS").actualShare, 1e-9)
        assertEquals(0.60, split.getValue("KEFOS").targetShare, 1e-9)
        assertEquals(0.25, split.getValue("QorFinder").actualShare, 1e-9)
        assertEquals(0.40, split.getValue("QorFinder").targetShare, 1e-9)
    }

    @Test
    fun `target shares are normalised when they don't add up to 100`() {
        val week = summary(emptyList(), listOf(kefos.copy(sharePercent = 30), qorFinder.copy(sharePercent = 30)))
        assertEquals(listOf(0.5, 0.5), week.split.map { it.targetShare })
        assertEquals(listOf(0.0, 0.0), week.split.map { it.actualShare })
    }

    @Test
    fun `an archived project shows only if it has time this week, with no target share`() {
        val old = Project(id = 3, name = "Old", sharePercent = 50, archived = true)
        val idleWeek = summary(emptyList(), listOf(kefos, qorFinder, old))
        assertEquals(listOf("KEFOS", "QorFinder"), idleWeek.split.map { it.project.name })

        val week = summary(listOf(engine(3, LocalDateTime.of(2026, 10, 12, 9, 0), 60)), listOf(kefos, qorFinder, old))
        val oldSplit = week.split.single { it.project.name == "Old" }
        assertEquals(1.0, oldSplit.actualShare, 1e-9)
        assertEquals(0.0, oldSplit.targetShare, 1e-9)
        assertEquals(0.6, week.split.single { it.project.name == "KEFOS" }.targetShare, 1e-9)
    }

    @Test
    fun `joy is totalled across the week`() {
        val start = clock.at(LocalDateTime.of(2026, 10, 14, 20, 0))
        val joy = Session(pillar = Pillar.JOY, projectId = null, startedAt = start, endedAt = start + 150 * minute)
        assertEquals(150 * minute, summary(listOf(joy)).joyMs)
    }
}
