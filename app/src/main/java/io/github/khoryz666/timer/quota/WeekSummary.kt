package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.day.DayTypeChoice
import io.github.khoryz666.timer.day.inForceOn
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Weeks start on Monday. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class DaySummary(
    val date: LocalDate,
    val dayType: DayType,
    val totals: DayTotals,
    /** Whole blocks per focus track. */
    val blocks: Map<Pillar, Int>,
    /** Target blocks per focus track, from [dayType]. */
    val targets: Map<Pillar, Int>,
)

data class TrackWeek(val pillar: Pillar, val trackedMs: Long, val blocks: Int, val targetBlocks: Int)

data class ProjectSplit(
    val project: Project,
    val trackedMs: Long,
    /** Share of the week's Project time, 0..1; 0 when there was no Project time. */
    val actualShare: Double,
    /** The project's [Project.sharePercent] normalised against the week's other projects, 0..1. */
    val targetShare: Double,
)

data class WeekSummary(
    val weekStart: LocalDate,
    val days: List<DaySummary>,
    val tracks: List<TrackWeek>,
    val split: List<ProjectSplit>,
) {
    val focusMs: Long get() = days.sumOf { it.totals.focusMs }
    val joyMs: Long get() = days.sumOf { it.totals.joyMs }

    fun track(pillar: Pillar): TrackWeek = tracks.single { it.pillar == pillar }

    companion object {
        /**
         * Summarises the Monday-start week beginning [weekStart]. [sessions] may include sessions
         * from outside the week; they are ignored. [plan] is the week's project plan; [projectNames]
         * names every project, so time on a project since taken out of the plan still shows.
         * [dayTypes] are the choices in force during the week (see [inForceOn]): a day with no
         * choice, future days included, keeps the latest earlier one. Weekly targets are the sum
         * of the days' targets.
         */
        fun of(
            weekStart: LocalDate,
            sessions: List<Session>,
            plan: List<Project>,
            projectNames: Map<Long, String>,
            dayTypes: List<DayTypeChoice>,
            settings: QuotaSettings,
            now: Long,
            clock: AppClock,
        ): WeekSummary {
            val totals = dailyTotals(sessions, now, clock)
            val days = (0L until 7L).map { offset ->
                val date = weekStart.plusDays(offset)
                val day = totals[date] ?: DayTotals()
                val type = dayTypes.inForceOn(date)
                DaySummary(
                    date = date,
                    dayType = type,
                    totals = day,
                    blocks = Pillar.FOCUS.associateWith { completedBlocks(day.ms(it), settings.blockMs) },
                    targets = Pillar.FOCUS.associateWith { settings.targets(type).target(it) },
                )
            }
            val tracks = Pillar.FOCUS.map { pillar ->
                TrackWeek(
                    pillar = pillar,
                    trackedMs = days.sumOf { it.totals.ms(pillar) },
                    // The sum of each day's whole blocks - leftover minutes don't carry over between days.
                    blocks = days.sumOf { it.blocks.getValue(pillar) },
                    targetBlocks = days.sumOf { it.targets.getValue(pillar) },
                )
            }
            return WeekSummary(weekStart, days, tracks, projectSplit(days, plan, projectNames))
        }

        private fun projectSplit(days: List<DaySummary>, plan: List<Project>, names: Map<Long, String>): List<ProjectSplit> {
            val byProject = days.flatMap { it.totals.byProject.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.sum() }
            val projectMs = byProject.values.sum()
            val shareTotal = plan.sumOf { it.sharePercent }
            val planned = plan.map { it.id }.toSet()
            val unplanned = byProject.keys.filterNot { it in planned }.sorted()
                .map { Project(it, names[it] ?: "Removed project", sharePercent = 0) }
            return (plan + unplanned).map { project ->
                val ms = byProject[project.id] ?: 0
                ProjectSplit(
                    project = project,
                    trackedMs = ms,
                    actualShare = if (projectMs == 0L) 0.0 else ms.toDouble() / projectMs,
                    targetShare = if (project.id !in planned || shareTotal == 0) 0.0 else project.sharePercent.toDouble() / shareTotal,
                )
            }
        }
    }
}
