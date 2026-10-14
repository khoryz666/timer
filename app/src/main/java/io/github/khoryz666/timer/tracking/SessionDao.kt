package io.github.khoryz666.timer.tracking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL LIMIT 1")
    abstract suspend fun running(): Session?

    /**
     * Sessions that end within [from, to) epoch ms, plus the running session (which ends "now",
     * so callers decide which day it belongs to).
     */
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL OR (endedAt >= :from AND endedAt < :to) ORDER BY startedAt")
    abstract fun observeEndingIn(from: Long, to: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL OR (endedAt >= :from AND endedAt < :to) ORDER BY startedAt")
    abstract suspend fun endingIn(from: Long, to: Long): List<Session>

    @Query("DELETE FROM sessions WHERE id = :id AND endedAt IS NOT NULL")
    abstract suspend fun deleteFinished(id: Long)

    /** Ends whatever is running at [now] and, unless [next] is null, starts [next] at the same instant. */
    @Transaction
    open suspend fun switchTo(next: TrackTarget?, now: Long) {
        endRunning(now)
        if (next != null) insert(Session.start(next, now))
    }

    // MAX() keeps a session from ending before it started if the clock was set back.
    @Query("UPDATE sessions SET endedAt = MAX(:now, startedAt) WHERE endedAt IS NULL")
    protected abstract suspend fun endRunning(now: Long)

    @Insert
    protected abstract suspend fun insert(session: Session): Long
}
