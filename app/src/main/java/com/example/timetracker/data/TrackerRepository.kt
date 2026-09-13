package com.example.timetracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for reading/switching the active timer state.
 * Shared by the UI (TrackerViewModel) and the notification quick actions
 * (TimeTrackerService) so both paths persist elapsed time identically.
 */
class TrackerRepository(
    private val prefs: TrackerPreferences,
    private val dao: TimeRecordDao
) {
    constructor(context: Context) : this(
        TrackerPreferences(context),
        LocalDatabase.getDatabase(context).timeRecordDao()
    )

    val activeStateFlow: Flow<ActiveState> = prefs.activeStateFlow
    val startTimeFlow: Flow<Long> = prefs.startTimeFlow
    fun allRecordsFlow(): Flow<List<TimeRecord>> = dao.getAllRecordsDescending()

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

    private fun currentDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
