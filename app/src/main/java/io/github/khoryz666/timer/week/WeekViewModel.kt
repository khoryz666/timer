package io.github.khoryz666.timer.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.day.DayTypeDao
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.quota.WeekSummary
import io.github.khoryz666.timer.quota.weekStart
import io.github.khoryz666.timer.settings.SettingsStore
import io.github.khoryz666.timer.tracking.Session
import io.github.khoryz666.timer.tracking.SessionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeekState(
    val summary: WeekSummary,
    val today: LocalDate,
    val now: Long,
    /** Sessions attributed to each day of the week. */
    val sessionsByDay: Map<LocalDate, List<Session>>,
    val projectNames: Map<Long, String>,
    /** The week's project plan. */
    val plan: List<Project>,
) {
    val isCurrentWeek: Boolean get() = today.weekStart() == summary.weekStart
    val shareTotal: Int get() = plan.sumOf { it.sharePercent }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(
    private val sessions: SessionDao,
    private val projects: ProjectDao,
    dayTypes: DayTypeDao,
    settings: SettingsStore,
    private val clock: AppClock,
) : ViewModel() {

    private val weekStart = MutableStateFlow(clock.today().weekStart())

    val state: StateFlow<WeekState?> = weekStart.flatMapLatest { start ->
        val range = clock.bounds(start, start.plusWeeks(1))
        val plan = combine(projects.observeWeek(start), projects.observeAll(), dayTypes.observeInForce(start, start.plusWeeks(1)), ::Triple)
        combine(
            sessions.observeEndingIn(range.start, range.end),
            plan,
            settings.quota,
            clock.ticks(60_000),
        ) { weekSessions, (weekPlan, allProjects, choices), quota, now ->
            val names = allProjects.associate { it.id to it.name }
            WeekState(
                summary = WeekSummary.of(start, weekSessions, weekPlan, names, choices, quota, now, clock),
                today = clock.dateOf(now),
                now = now,
                sessionsByDay = weekSessions.groupBy { clock.dateOf(it.endOrNow(now)) },
                projectNames = names,
                plan = weekPlan,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousWeek() = weekStart.update { it.minusWeeks(1) }

    fun nextWeek() = weekStart.update { minOf(it.plusWeeks(1), clock.today().weekStart()) }

    fun delete(session: Session) {
        viewModelScope.launch { sessions.deleteFinished(session.id) }
    }

    // Project edits only ever apply to the current week; earlier weeks keep their own plans.

    fun addProject(name: String) = editThisWeek { projects.add(it, name.trim()) }

    fun renameProject(project: Project, name: String) = editThisWeek { projects.rename(project.id, name.trim()) }

    fun setShare(project: Project, percent: Int) = editThisWeek { projects.setShare(it, project.id, percent) }

    fun removeProject(project: Project) = editThisWeek { projects.remove(it, project.id) }

    private fun editThisWeek(edit: suspend (LocalDate) -> Unit) {
        viewModelScope.launch { edit(clock.today().weekStart()) }
    }
}
