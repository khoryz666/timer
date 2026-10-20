package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.day.DayTypeChoice
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DayTargets
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.JoyProgress
import io.github.khoryz666.timer.quota.QuotaSettings
import io.github.khoryz666.timer.quota.TrackProgress
import java.time.LocalDate

/** Everything the Today screen and the notification show, computed at [now]. */
data class TodayStatus(
    val date: LocalDate,
    val now: Long,
    val running: Session?,
    val dayType: DayType,
    /** False until today's day type is picked; until then [dayType] is the latest earlier choice. */
    val dayTypeChosen: Boolean,
    val targets: DayTargets,
    /** This week's projects, in order. */
    val projects: List<Project>,
    /** Name of the running session's project (it may since have left this week's plan). */
    val runningProjectName: String?,
    val totals: DayTotals,
    /** Project, NeetCode and Reading, in that order. */
    val tracks: List<TrackProgress>,
    val joy: JoyProgress,
) {
    fun track(pillar: Pillar): TrackProgress = tracks.single { it.pillar == pillar }

    companion object {
        fun of(
            date: LocalDate,
            now: Long,
            running: Session?,
            dayType: DayTypeChoice?,
            projects: List<Project>,
            projectNames: Map<Long, String>,
            totals: DayTotals,
            settings: QuotaSettings,
        ): TodayStatus {
            val type = dayType?.type ?: DayType.DEFAULT
            val targets = settings.targets(type)
            return TodayStatus(
                date = date,
                now = now,
                running = running,
                dayType = type,
                dayTypeChosen = dayType?.date == date,
                targets = targets,
                projects = projects,
                runningProjectName = running?.projectId?.let(projectNames::get),
                totals = totals,
                tracks = Pillar.FOCUS.map { TrackProgress.of(it, totals.ms(it), targets, settings.blockMs) },
                joy = JoyProgress.of(totals.joyMs, settings),
            )
        }
    }
}
