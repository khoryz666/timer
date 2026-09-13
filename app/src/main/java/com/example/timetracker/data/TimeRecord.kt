package com.example.timetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class TimeRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val workDurationMs: Long = 0L,
    val selfDurationMs: Long = 0L,
    val sleepDurationMs: Long = 0L
) {
    fun plusDuration(state: ActiveState, amountMs: Long): TimeRecord = when (state) {
        ActiveState.WORK -> copy(workDurationMs = workDurationMs + amountMs)
        ActiveState.SELF -> copy(selfDurationMs = selfDurationMs + amountMs)
        ActiveState.SLEEP -> copy(sleepDurationMs = sleepDurationMs + amountMs)
        ActiveState.IDLE -> this
    }

    fun withZeroed(state: ActiveState): TimeRecord = when (state) {
        ActiveState.WORK -> copy(workDurationMs = 0L)
        ActiveState.SELF -> copy(selfDurationMs = 0L)
        ActiveState.SLEEP -> copy(sleepDurationMs = 0L)
        ActiveState.IDLE -> this
    }
}
