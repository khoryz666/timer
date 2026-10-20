package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.day.DayTypeChoice
import io.github.khoryz666.timer.day.DayTypeDao
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.dailyTotals
import io.github.khoryz666.timer.quota.weekStart
import io.github.khoryz666.timer.settings.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Starts, stops and switches sessions, sets the day type, and reports today's progress. The
 * app and the notifications all go through here. All state lives in the database, so tracking
 * carries on across process death and reboots: a running session is just a row with no end time.
 */
class Tracker(
    private val sessions: SessionDao,
    private val projects: ProjectDao,
    private val dayTypes: DayTypeDao,
    private val settings: SettingsStore,
    private val clock: AppClock,
    private val notices: TrackerNotices,
) {
    /** Ends the running session (if any) and starts [target]. Does nothing if [target] is already running. */
    suspend fun switchTo(target: TrackTarget) {
        if (sessions.running()?.target == target) return
        val now = clock.now()
        if (shouldNudge(settings.isNudgeEnabled(), target, focusBlocksToday(now))) {
            notices.nudgeJoyBeforeFocus()
        }
        sessions.switchTo(target, now)
    }

    suspend fun stop() = sessions.switchTo(null, clock.now())

    /** Sets today's day type; days after today keep it until they get their own. */
    suspend fun setDayType(type: DayType) {
        dayTypes.set(DayTypeChoice(clock.today(), type))
        notices.dismissDayPrompt()
    }

    /**
     * Today's status, re-emitted whenever sessions, projects, the day type or settings change,
     * every [tickMs] while something is running, and when the date rolls over.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun today(tickMs: Long): Flow<TodayStatus> =
        clock.ticks(DATE_CHECK_MS).map { clock.today() }.distinctUntilChanged().flatMapLatest { date ->
            val day = clock.dayBounds(date)
            combine(
                sessions.observeEndingIn(day.start, day.end),
                projects.observeWeek(date.weekStart()),
                projects.observeAll(),
                dayTypes.observeInForce(date),
                settings.quota,
            ) { daySessions, plan, allProjects, dayType, quota ->
                val running = daySessions.find { it.isRunning }
                val names = allProjects.associate { it.id to it.name }
                running to { now: Long ->
                    val totals = dailyTotals(daySessions, now, clock)[date] ?: DayTotals()
                    TodayStatus.of(date, now, running, dayType, plan, names, totals, quota)
                }
            }.flatMapLatest { (running, statusAt) ->
                val nowFlow = if (running != null) clock.ticks(tickMs) else flowOf(clock.now())
                nowFlow.map(statusAt)
            }
        }

    private suspend fun focusBlocksToday(now: Long): Int {
        val date = clock.dateOf(now)
        val day = clock.dayBounds(date)
        val totals = dailyTotals(sessions.endingIn(day.start, day.end), now, clock)[date] ?: return 0
        return totals.focusBlocks(settings.quota.first().blockMs)
    }

    private companion object {
        const val DATE_CHECK_MS = 60_000L
    }
}
