package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import java.time.LocalDate

/** Tracked time for one local day. */
data class DayTotals(
    val engineMs: Long = 0,
    val joyMs: Long = 0,
    val engineByProject: Map<Long, Long> = emptyMap(),
) {
    fun with(session: Session, ms: Long): DayTotals = when (session.pillar) {
        Pillar.JOY -> copy(joyMs = joyMs + ms)
        Pillar.ENGINE -> {
            val projectId = requireNotNull(session.projectId)
            copy(
                engineMs = engineMs + ms,
                engineByProject = engineByProject + (projectId to (engineByProject[projectId] ?: 0) + ms),
            )
        }
    }
}

/**
 * Groups sessions by the local day they end on - a session that crosses midnight counts entirely
 * toward the later day. The running session ends "now", so it counts toward today.
 */
fun dailyTotals(sessions: List<Session>, now: Long, clock: AppClock): Map<LocalDate, DayTotals> =
    sessions.groupBy { clock.dateOf(it.endOrNow(now)) }
        .mapValues { (_, daySessions) -> daySessions.fold(DayTotals()) { acc, s -> acc.with(s, s.durationMs(now)) } }
