package io.github.khoryz666.timer.tracking

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Pillar { ENGINE, JOY }

/** What a session is tracking. Engine time always belongs to a project. */
sealed interface TrackTarget {
    data class Engine(val projectId: Long) : TrackTarget
    data object Joy : TrackTarget
}

/**
 * One stretch of tracked time. A session with no [endedAt] is the one running now; there is
 * at most one. Times are epoch milliseconds. A session counts toward the local day it ends on.
 */
@Entity(tableName = "sessions", indices = [Index("endedAt")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillar: Pillar,
    val projectId: Long?,
    val startedAt: Long,
    val endedAt: Long? = null,
) {
    val isRunning: Boolean get() = endedAt == null

    val target: TrackTarget
        get() = when (pillar) {
            Pillar.ENGINE -> TrackTarget.Engine(requireNotNull(projectId))
            Pillar.JOY -> TrackTarget.Joy
        }

    /** When this session ends, or [now] while it is still running. */
    fun endOrNow(now: Long): Long = endedAt ?: now

    /** Never negative, even if the clock was set back while it ran. */
    fun durationMs(now: Long): Long = (endOrNow(now) - startedAt).coerceAtLeast(0)

    companion object {
        fun start(target: TrackTarget, at: Long) = when (target) {
            is TrackTarget.Engine -> Session(pillar = Pillar.ENGINE, projectId = target.projectId, startedAt = at)
            TrackTarget.Joy -> Session(pillar = Pillar.JOY, projectId = null, startedAt = at)
        }
    }
}
