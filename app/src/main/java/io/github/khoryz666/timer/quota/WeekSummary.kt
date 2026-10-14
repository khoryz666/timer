package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.tracking.Session
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Weeks start on Monday. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class DaySummary(val date: LocalDate, val totals: DayTotals, val engineBlocks: Int)

data class ProjectSplit(
    val project: Project,
    val trackedMs: Long,
    /** Share of the week's Engine time, 0..1; 0 when there was no Engine time. */
    val actualShare: Double,
    /** The project's [Project.sharePercent] normalised against the other active projects, 0..1. */
    val targetShare: Double,
)

data class WeekSummary(
    val weekStart: LocalDate,
    val days: List<DaySummary>,
    val weeklyTargetBlocks: Int,
    val split: List<ProjectSplit>,
) {
    /** The sum of each day's whole blocks - leftover minutes don't carry over between days. */
    val engineBlocks: Int get() = days.sumOf { it.engineBlocks }
    val engineMs: Long get() = days.sumOf { it.totals.engineMs }
    val joyMs: Long get() = days.sumOf { it.totals.joyMs }

    companion object {
        /**
         * Summarises the Monday-start week beginning [weekStart]. [sessions] may include sessions
         * from outside the week; they are ignored. [projects] should include archived projects so
         * their past time still shows up.
         */
        fun of(
            weekStart: LocalDate,
            sessions: List<Session>,
            projects: List<Project>,
            settings: QuotaSettings,
            now: Long,
            clock: AppClock,
        ): WeekSummary {
            val totals = dailyTotals(sessions, now, clock)
            val days = (0L until 7L).map { offset ->
                val date = weekStart.plusDays(offset)
                val day = totals[date] ?: DayTotals()
                DaySummary(date, day, completedBlocks(day.engineMs, settings.blockMs))
            }
            return WeekSummary(weekStart, days, settings.weeklyTargetBlocks, projectSplit(days, projects))
        }

        private fun projectSplit(days: List<DaySummary>, projects: List<Project>): List<ProjectSplit> {
            val byProject = days.flatMap { it.totals.engineByProject.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.sum() }
            val engineMs = byProject.values.sum()
            val shareTotal = projects.filterNot { it.archived }.sumOf { it.sharePercent }
            return projects
                .filter { !it.archived || (byProject[it.id] ?: 0) > 0 }
                .map { project ->
                    val ms = byProject[project.id] ?: 0
                    ProjectSplit(
                        project = project,
                        trackedMs = ms,
                        actualShare = if (engineMs == 0L) 0.0 else ms.toDouble() / engineMs,
                        targetShare = if (project.archived || shareTotal == 0) 0.0 else project.sharePercent.toDouble() / shareTotal,
                    )
                }
        }
    }
}
