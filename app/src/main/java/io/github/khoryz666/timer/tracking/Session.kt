package io.github.khoryz666.timer.tracking

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** What time goes to. Project, NeetCode and Reading are focus tracks counted in blocks; Joy is leisure. */
enum class Pillar(val label: String, val shortLabel: String) {
    PROJECT("Project", "Project"),
    NEETCODE("NeetCode", "Neet"),
    READING("Reading", "Read"),
    JOY("Joy", "Joy");

    val isFocus: Boolean get() = this != JOY

    companion object {
        val FOCUS = listOf(PROJECT, NEETCODE, READING)
    }
}

/** What a session is tracking. Project time always belongs to one of the week's projects. */
sealed interface TrackTarget {
    val pillar: Pillar

    data class Project(val projectId: Long) : TrackTarget {
        override val pillar get() = Pillar.PROJECT
    }

    data object NeetCode : TrackTarget {
        override val pillar get() = Pillar.NEETCODE
    }

    data object Reading : TrackTarget {
        override val pillar get() = Pillar.READING
    }

    data object Joy : TrackTarget {
        override val pillar get() = Pillar.JOY
    }

    companion object {
        fun of(pillar: Pillar, projectId: Long?): TrackTarget = when (pillar) {
            Pillar.PROJECT -> Project(requireNotNull(projectId))
            Pillar.NEETCODE -> NeetCode
            Pillar.READING -> Reading
            Pillar.JOY -> Joy
        }
    }
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

    val target: TrackTarget get() = TrackTarget.of(pillar, projectId)

    /** When this session ends, or [now] while it is still running. */
    fun endOrNow(now: Long): Long = endedAt ?: now

    /** Never negative, even if the clock was set back while it ran. */
    fun durationMs(now: Long): Long = (endOrNow(now) - startedAt).coerceAtLeast(0)

    companion object {
        fun start(target: TrackTarget, at: Long) =
            Session(pillar = target.pillar, projectId = (target as? TrackTarget.Project)?.projectId, startedAt = at)
    }
}
