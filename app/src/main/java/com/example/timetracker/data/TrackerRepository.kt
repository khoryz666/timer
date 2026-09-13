package com.example.timetracker.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A fully computed, display-ready view of the tracker's current state. */
data class TrackerSnapshot(
    val activeState: ActiveState = ActiveState.IDLE,
    val workDurationMs: Long = 0L,
    val selfDurationMs: Long = 0L,
    val sleepDurationMs: Long = 0L,
    val activelyTickingMs: Long = 0L
)

/**
 * Single source of truth for reading/switching the active timer state.
 * Shared by the UI (TrackerViewModel) and the notification quick actions
 * (TimeTrackerService) so both paths persist elapsed time - and compute
 * what's currently on screen/in the notification - identically.
 */
class TrackerRepository(
    private val prefs: TrackerPreferences,
    private val dao: TimeRecordDao
) {
    constructor(context: Context) : this(
        TrackerPreferences(context),
        LocalDatabase.getDatabase(context).timeRecordDao()
    )

    fun allRecordsFlow(): Flow<List<TimeRecord>> = dao.getAllRecordsDescending()

    /**
     * Emits a [TrackerSnapshot] for whatever is happening right now, re-emitting every
     * [tickIntervalMs] while a category is active so the ticking total stays live; holds
     * steady (no ticking) while idle.
     */
    fun snapshotFlow(tickIntervalMs: Long = 1_000L): Flow<TrackerSnapshot> =
        combine(prefs.activeStateFlow, prefs.startTimeFlow, dao.getAllRecordsDescending()) { state, startTime, records ->
            val record = records.find { it.date == currentDateString() }
            Triple(state, startTime, record)
        }.transformLatest { (state, startTime, record) ->
            if (state == ActiveState.IDLE) {
                emit(record.toSnapshot(state, activelyTickingMs = 0L))
            } else {
                while (true) {
                    emit(record.toSnapshot(state, activelyTickingMs = System.currentTimeMillis() - startTime))
                    delay(tickIntervalMs)
                }
            }
        }

    suspend fun switchState(newState: ActiveState) {
        val currentState = prefs.activeStateFlow.first()
        val startTime = prefs.startTimeFlow.first()
        val now = System.currentTimeMillis()

        if (currentState != ActiveState.IDLE) {
            saveElapsedTimeToDb(currentState, startTime, now)
        }

        prefs.setActiveState(newState, now)
    }

    suspend fun forceSaveCurrentProgress() {
        val currentState = prefs.activeStateFlow.first()
        val startTime = prefs.startTimeFlow.first()
        val now = System.currentTimeMillis()

        if (currentState != ActiveState.IDLE) {
            saveElapsedTimeToDb(currentState, startTime, now)
            prefs.setActiveState(currentState, now)
        }
    }

    suspend fun resetActiveTimer() {
        val currentState = prefs.activeStateFlow.first()
        if (currentState == ActiveState.IDLE) return

        prefs.setActiveState(currentState, System.currentTimeMillis())

        val dateStr = currentDateString()
        val record = dao.getRecordByDate(dateStr) ?: TimeRecord(date = dateStr)
        val updatedRecord = when (currentState) {
            ActiveState.WORK -> record.copy(workDurationMs = 0L)
            ActiveState.SELF -> record.copy(selfDurationMs = 0L)
            ActiveState.SLEEP -> record.copy(sleepDurationMs = 0L)
            else -> record
        }
        dao.insertOrUpdate(updatedRecord)
    }

    private suspend fun saveElapsedTimeToDb(state: ActiveState, startTime: Long, endTime: Long) {
        val elapsed = endTime - startTime
        if (elapsed <= 0) return

        // Resolve Midnight Rollover: Save entirely to endTime's date
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime))
        val currentRecord = dao.getRecordByDate(dateStr) ?: TimeRecord(date = dateStr)

        val updatedRecord = when (state) {
            ActiveState.WORK -> currentRecord.copy(workDurationMs = currentRecord.workDurationMs + elapsed)
            ActiveState.SELF -> currentRecord.copy(selfDurationMs = currentRecord.selfDurationMs + elapsed)
            ActiveState.SLEEP -> currentRecord.copy(sleepDurationMs = currentRecord.sleepDurationMs + elapsed)
            else -> currentRecord
        }
        dao.insertOrUpdate(updatedRecord)
    }

    private fun TimeRecord?.toSnapshot(state: ActiveState, activelyTickingMs: Long) = TrackerSnapshot(
        activeState = state,
        workDurationMs = this?.workDurationMs ?: 0L,
        selfDurationMs = this?.selfDurationMs ?: 0L,
        sleepDurationMs = this?.sleepDurationMs ?: 0L,
        activelyTickingMs = activelyTickingMs
    )

    private fun currentDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
