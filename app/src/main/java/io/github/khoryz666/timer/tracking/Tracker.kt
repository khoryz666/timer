package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.completedBlocks
import io.github.khoryz666.timer.quota.dailyTotals
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
 * Starts, stops and switches sessions, and reports today's progress. The app and the
 * notification both go through here. All state lives in the database, so tracking carries
 * on across process death and reboots: a running session is just a row with no end time.
 */
class Tracker(
    private val sessions: SessionDao,
    private val projects: ProjectDao,
    private val settings: SettingsStore,
    private val clock: AppClock,
    private val nudger: Nudger,
) {
    /** Ends the running session (if any) and starts [target]. Does nothing if [target] is already running. */
    suspend fun switchTo(target: TrackTarget) {
        if (sessions.running()?.target == target) return
        val now = clock.now()
        if (shouldNudge(settings.isNudgeEnabled(), target, engineBlocksToday(now))) {
            nudger.nudgeJoyBeforeEngine()
        }
        sessions.switchTo(target, now)
    }

    suspend fun stop() = sessions.switchTo(null, clock.now())

    /**
     * Today's status, re-emitted whenever sessions, projects or settings change, every
     * [tickMs] while something is running, and when the date rolls over.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun today(tickMs: Long): Flow<TodayStatus> =
        clock.ticks(DATE_CHECK_MS).map { clock.today() }.distinctUntilChanged().flatMapLatest { date ->
            val day = clock.dayBounds(date)
            combine(
                sessions.observeEndingIn(day.start, day.end),
                projects.observeAll(),
                settings.quota,
                ::Triple,
            ).flatMapLatest { (daySessions, allProjects, quota) ->
                val running = daySessions.find { it.isRunning }
                val nowFlow = if (running != null) clock.ticks(tickMs) else flowOf(clock.now())
                nowFlow.map { now ->
                    val totals = dailyTotals(daySessions, now, clock)[date] ?: DayTotals()
                    TodayStatus.of(date, now, running, allProjects, totals, quota)
                }
            }
        }

    private suspend fun engineBlocksToday(now: Long): Int {
        val date = clock.dateOf(now)
        val day = clock.dayBounds(date)
        val totals = dailyTotals(sessions.endingIn(day.start, day.end), now, clock)[date] ?: return 0
        return completedBlocks(totals.engineMs, settings.quota.first().blockMs)
    }

    private companion object {
        const val DATE_CHECK_MS = 60_000L
    }
}
