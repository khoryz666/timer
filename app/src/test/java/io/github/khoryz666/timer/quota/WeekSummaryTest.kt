package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.FixedClock
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.day.DayTypeChoice
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
    private val qorFinder = Project(id = 1, name = "QorFinder", sharePercent = 80)
    private val kefos = Project(id = 2, name = "KEFOS", sharePercent = 20)
    private val names = mapOf(1L to "QorFinder", 2L to "KEFOS", 3L to "Old")

    private fun session(pillar: Pillar, from: LocalDateTime, minutes: Long, projectId: Long? = null): Session {
        val start = clock.at(from)
        return Session(pillar = pillar, projectId = projectId, startedAt = start, endedAt = start + minutes * minute)
    }

    private fun project(projectId: Long, from: LocalDateTime, minutes: Long) = session(Pillar.PROJECT, from, minutes, projectId)

    private fun summary(
        sessions: List<Session>,
        plan: List<Project> = listOf(qorFinder, kefos),
        dayTypes: List<DayTypeChoice> = listOf(DayTypeChoice(monday, DayType.DEEP)),
    ) = WeekSummary.of(monday, sessions, plan, names, dayTypes, QuotaSettings(), clock.now(), clock)

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
    fun `weekly blocks are the sum of each day's whole blocks, per track`() {
        val week = summary(
            listOf(
                project(1, LocalDateTime.of(2026, 10, 12, 9, 0), 150), //  Mon: 2 blocks + 30 min
                project(2, LocalDateTime.of(2026, 10, 13, 9, 0), 50), //   Tue: 0 blocks
                project(1, LocalDateTime.of(2026, 10, 14, 9, 0), 300), //  Wed: 5 blocks
                session(Pillar.READING, LocalDateTime.of(2026, 10, 14, 15, 0), 130), // Wed: 2 blocks
            )
        )

        assertEquals(listOf(2, 0, 5, 0, 0, 0, 0), week.days.map { it.blocks.getValue(Pillar.PROJECT) })
        assertEquals(7, week.track(Pillar.PROJECT).blocks) // not 500 / 60 = 8: leftovers don't carry over
        assertEquals(500 * minute, week.track(Pillar.PROJECT).trackedMs)
        assertEquals(2, week.track(Pillar.READING).blocks)
        assertEquals(630 * minute, week.focusMs)
    }

    @Test
    fun `each day keeps the latest day type chosen on or before it, and weekly targets add up the days'`() {
        val week = summary(
            emptyList(),
            dayTypes = listOf(
                DayTypeChoice(monday.minusDays(3), DayType.LIGHT), // in force on Monday
                DayTypeChoice(monday.plusDays(1), DayType.DEEP), //    Tue-Thu
                DayTypeChoice(monday.plusDays(4), DayType.CLASS), //   Fri-Sun
            ),
        )

        assertEquals(
            listOf(DayType.LIGHT, DayType.DEEP, DayType.DEEP, DayType.DEEP, DayType.CLASS, DayType.CLASS, DayType.CLASS),
            week.days.map { it.dayType },
        )
        assertEquals(3 + 5 * 3 + 3 * 3, week.track(Pillar.PROJECT).targetBlocks)
        assertEquals(0 + 1 * 3 + 1 * 3, week.track(Pillar.NEETCODE).targetBlocks)
        assertEquals(0 + 2 * 3 + 1 * 3, week.track(Pillar.READING).targetBlocks)
    }

    @Test
    fun `with no day type ever chosen every day uses the default`() {
        val week = summary(emptyList(), dayTypes = emptyList())
        assertEquals(List(7) { DayType.DEFAULT }, week.days.map { it.dayType })
    }

    @Test
    fun `sessions ending outside the week are ignored, including a Sunday-night session that ends on Monday`() {
        val week = summary(
            listOf(
                project(1, LocalDateTime.of(2026, 10, 11, 23, 0), 120), // Sun -> Mon 01:00: counts Monday
                project(1, LocalDateTime.of(2026, 10, 18, 23, 30), 60), //  Sun -> next Mon: next week
                project(1, LocalDateTime.of(2026, 10, 5, 9, 0), 90), //    previous week
            )
        )

        assertEquals(120 * minute, week.days.first().totals.ms(Pillar.PROJECT))
        assertEquals(120 * minute, week.focusMs)
    }

    @Test
    fun `project split compares actual share of Project time with the week's target share`() {
        val week = summary(
            listOf(
                project(1, LocalDateTime.of(2026, 10, 12, 9, 0), 270),
                project(2, LocalDateTime.of(2026, 10, 13, 9, 0), 90),
                session(Pillar.NEETCODE, LocalDateTime.of(2026, 10, 13, 7, 0), 60), // not Project time
            )
        )

        val split = week.split.associateBy { it.project.name }
        assertEquals(0.75, split.getValue("QorFinder").actualShare, 1e-9)
        assertEquals(0.80, split.getValue("QorFinder").targetShare, 1e-9)
        assertEquals(0.25, split.getValue("KEFOS").actualShare, 1e-9)
        assertEquals(0.20, split.getValue("KEFOS").targetShare, 1e-9)
    }

    @Test
    fun `target shares are normalised when they don't add up to 100`() {
        val week = summary(emptyList(), listOf(qorFinder.copy(sharePercent = 30), kefos.copy(sharePercent = 30)))
        assertEquals(listOf(0.5, 0.5), week.split.map { it.targetShare })
        assertEquals(listOf(0.0, 0.0), week.split.map { it.actualShare })
    }

    @Test
    fun `a project not in the week's plan shows only if it has time this week, with no target share`() {
        assertEquals(listOf("QorFinder", "KEFOS"), summary(emptyList()).split.map { it.project.name })

        val week = summary(listOf(project(3, LocalDateTime.of(2026, 10, 12, 9, 0), 60)))
        val old = week.split.single { it.project.name == "Old" }
        assertEquals(1.0, old.actualShare, 1e-9)
        assertEquals(0.0, old.targetShare, 1e-9)
        assertEquals(0.8, week.split.single { it.project.name == "QorFinder" }.targetShare, 1e-9)
    }

    @Test
    fun `joy is totalled across the week`() {
        val joy = session(Pillar.JOY, LocalDateTime.of(2026, 10, 14, 20, 0), 150)
        assertEquals(150 * minute, summary(listOf(joy)).joyMs)
        assertEquals(0L, summary(listOf(joy)).focusMs)
    }
}
