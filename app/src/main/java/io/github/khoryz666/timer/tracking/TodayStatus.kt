package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.EngineProgress
import io.github.khoryz666.timer.quota.JoyProgress
import io.github.khoryz666.timer.quota.QuotaSettings
import java.time.LocalDate

/** Everything the Today screen and the notification show, computed at [now]. */
data class TodayStatus(
    val date: LocalDate,
    val now: Long,
    val running: Session?,
    /** Active projects, in order. */
    val projects: List<Project>,
    /** Name of the running session's project (it may since have been archived). */
    val runningProjectName: String?,
    val totals: DayTotals,
    val engine: EngineProgress,
    val joy: JoyProgress,
) {
    companion object {
        fun of(date: LocalDate, now: Long, running: Session?, allProjects: List<Project>, totals: DayTotals, settings: QuotaSettings) =
            TodayStatus(
                date = date,
                now = now,
                running = running,
                projects = allProjects.filterNot { it.archived },
                runningProjectName = running?.projectId?.let { id -> allProjects.find { it.id == id }?.name },
                totals = totals,
                engine = EngineProgress.of(totals.engineMs, settings),
                joy = JoyProgress.of(totals.joyMs, settings),
            )
    }
}
