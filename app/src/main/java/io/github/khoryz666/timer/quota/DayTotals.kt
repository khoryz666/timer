package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import java.time.LocalDate

/** Tracked time for one local day. */
data class DayTotals(
    val byPillar: Map<Pillar, Long> = emptyMap(),
    val byProject: Map<Long, Long> = emptyMap(),
) {
    fun ms(pillar: Pillar): Long = byPillar[pillar] ?: 0

    val joyMs: Long get() = ms(Pillar.JOY)
    val focusMs: Long get() = Pillar.FOCUS.sumOf(::ms)

    fun with(session: Session, ms: Long): DayTotals {
        val pillars = byPillar + (session.pillar to ms(session.pillar) + ms)
        if (session.pillar != Pillar.PROJECT) return copy(byPillar = pillars)
        val projectId = requireNotNull(session.projectId)
        return DayTotals(pillars, byProject + (projectId to (byProject[projectId] ?: 0) + ms))
    }

    /** Each focus track's whole blocks; leftover minutes don't carry over between tracks. */
    fun focusBlocks(blockMs: Long): Int = Pillar.FOCUS.sumOf { completedBlocks(ms(it), blockMs) }
}

/**
 * Groups sessions by the local day they end on - a session that crosses midnight counts entirely
 * toward the later day. The running session ends "now", so it counts toward today.
 */
fun dailyTotals(sessions: List<Session>, now: Long, clock: AppClock): Map<LocalDate, DayTotals> =
    sessions.groupBy { clock.dateOf(it.endOrNow(now)) }
        .mapValues { (_, daySessions) -> daySessions.fold(DayTotals()) { acc, s -> acc.with(s, s.durationMs(now)) } }
