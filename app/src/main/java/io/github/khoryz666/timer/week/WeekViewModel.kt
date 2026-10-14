package io.github.khoryz666.timer.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.core.AppClock
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
) {
    val isCurrentWeek: Boolean get() = today.weekStart() == summary.weekStart
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(
    private val sessions: SessionDao,
    projects: ProjectDao,
    settings: SettingsStore,
    private val clock: AppClock,
) : ViewModel() {

    private val weekStart = MutableStateFlow(clock.today().weekStart())

    val state: StateFlow<WeekState?> = weekStart.flatMapLatest { start ->
        val range = clock.bounds(start, start.plusWeeks(1))
        combine(
            sessions.observeEndingIn(range.start, range.end),
            projects.observeAll(),
            settings.quota,
            clock.ticks(60_000),
        ) { weekSessions, allProjects, quota, now ->
            WeekState(
                summary = WeekSummary.of(start, weekSessions, allProjects, quota, now, clock),
                today = clock.dateOf(now),
                now = now,
                sessionsByDay = weekSessions.groupBy { clock.dateOf(it.endOrNow(now)) },
                projectNames = allProjects.associate { it.id to it.name },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousWeek() = weekStart.update { it.minusWeeks(1) }

    fun nextWeek() = weekStart.update { minOf(it.plusWeeks(1), clock.today().weekStart()) }

    fun delete(session: Session) {
        viewModelScope.launch { sessions.deleteFinished(session.id) }
    }
}
