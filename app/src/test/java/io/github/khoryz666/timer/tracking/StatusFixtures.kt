package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.day.DayTypeChoice
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.QuotaSettings
import java.time.LocalDate

/** A Wednesday with this week's projects QorFinder (80%) and KEFOS (20%), 60-min blocks. */
object StatusFixtures {
    const val MINUTE = 60_000L
    val date: LocalDate = LocalDate.of(2026, 10, 14)
    val projects = listOf(Project(1, "QorFinder", 80), Project(2, "KEFOS", 20))

    fun totals(project: Long = 0, neetCode: Long = 0, reading: Long = 0, joy: Long = 0) = DayTotals(
        byPillar = mapOf(
            Pillar.PROJECT to project * MINUTE,
            Pillar.NEETCODE to neetCode * MINUTE,
            Pillar.READING to reading * MINUTE,
            Pillar.JOY to joy * MINUTE,
        ),
        byProject = if (project > 0) mapOf(1L to project * MINUTE) else emptyMap(),
    )

    fun status(
        running: Session? = null,
        totals: DayTotals = DayTotals(),
        dayType: DayType = DayType.DEEP,
        chosen: Boolean = true,
        now: Long = 0,
        projects: List<Project> = this.projects,
    ) = TodayStatus.of(
        date = date,
        now = now,
        running = running,
        dayType = DayTypeChoice(if (chosen) date else date.minusDays(1), dayType),
        projects = projects,
        projectNames = this.projects.associate { it.id to it.name },
        totals = totals,
        settings = QuotaSettings(),
    )

    fun running(target: TrackTarget, startedAt: Long = 1234) = Session.start(target, startedAt)
}
