package com.example.timetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.LocalDatabase
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TrackerPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackerUiState(
    val activeState: ActiveState = ActiveState.IDLE,
    val workDurationMs: Long = 0L,
    val selfDurationMs: Long = 0L,
    val sleepDurationMs: Long = 0L,
    val activelyTickingMs: Long = 0L
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LocalDatabase.getDatabase(application)
    private val timeRecordDao = database.timeRecordDao()
    private val prefs = TrackerPreferences(application)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private var todayRecord: TimeRecord? = null
    private var timerJob: Job? = null

    init {
        // Collect state from preferences and local DB
        viewModelScope.launch {
            combine(
                prefs.activeStateFlow,
                prefs.startTimeFlow,
                timeRecordDao.getAllRecordsDescending()
            ) { state, startTime, records ->
                val dateStr = getCurrentDateString()
                val currentRecord = records.find { it.date == dateStr } 
                    ?: TimeRecord(date = dateStr)
                
                todayRecord = currentRecord
                
                Triple(state, startTime, currentRecord)
            }.collect { (state, startTime, record) ->
                _uiState.value = TrackerUiState(
                    activeState = state,
                    workDurationMs = record.workDurationMs,
                    selfDurationMs = record.selfDurationMs,
                    sleepDurationMs = record.sleepDurationMs,
                    activelyTickingMs = if (state != ActiveState.IDLE) System.currentTimeMillis() - startTime else 0L
                )
                
                if (state != ActiveState.IDLE && timerJob?.isActive != true) {
                    startTicking()
                } else if (state == ActiveState.IDLE) {
                    timerJob?.cancel()
                }
            }
        }
    }

    private fun startTicking() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val state = prefs.activeStateFlow.first()
                if (state != ActiveState.IDLE) {
                    val startTime = prefs.startTimeFlow.first()
                    _uiState.value = _uiState.value.copy(
                        activelyTickingMs = System.currentTimeMillis() - startTime
                    )
                }
                delay(1000L) // Update UI every second
            }
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun onButtonPress(newState: ActiveState) {
        viewModelScope.launch {
            val currentState = prefs.activeStateFlow.first()
            val startTime = prefs.startTimeFlow.first()
            val now = System.currentTimeMillis()

            if (currentState != ActiveState.IDLE) {
                 saveElapsedTimeToDb(currentState, startTime, now)
            }

            prefs.setActiveState(newState, now)
        }
    }

    fun resetActiveTimer() {
        viewModelScope.launch {
             val currentState = prefs.activeStateFlow.first()
             if (currentState == ActiveState.IDLE) return@launch
             
             // Reset start time to now without changing state
             prefs.setActiveState(currentState, System.currentTimeMillis())
             
             // Clear today's database record for this section
             val dateStr = getCurrentDateString()
             val record = timeRecordDao.getRecordByDate(dateStr) ?: TimeRecord(date = dateStr)
             val updatedRecord = when (currentState) {
                 ActiveState.WORK -> record.copy(workDurationMs = 0L)
                 ActiveState.SELF -> record.copy(selfDurationMs = 0L)
                 ActiveState.SLEEP -> record.copy(sleepDurationMs = 0L)
                 else -> record
             }
             timeRecordDao.insertOrUpdate(updatedRecord)
        }
    }

    fun forceSaveCurrentProgress() {
        viewModelScope.launch {
            val currentState = prefs.activeStateFlow.first()
            val startTime = prefs.startTimeFlow.first()
            val now = System.currentTimeMillis()

            if (currentState != ActiveState.IDLE) {
                 // Save the time up until now
                 saveElapsedTimeToDb(currentState, startTime, now)
                 // Start a new session from right now
                 prefs.setActiveState(currentState, now)
            }
        }
    }

    private suspend fun saveElapsedTimeToDb(state: ActiveState, startTime: Long, endTime: Long) {
        val elapsed = endTime - startTime
        if (elapsed <= 0) return

        // Resolve Midnight Rollover: Save entirely to endTime's date
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime))
        val currentRecord = timeRecordDao.getRecordByDate(dateStr) ?: TimeRecord(date = dateStr)

        val updatedRecord = when (state) {
            ActiveState.WORK -> currentRecord.copy(workDurationMs = currentRecord.workDurationMs + elapsed)
            ActiveState.SELF -> currentRecord.copy(selfDurationMs = currentRecord.selfDurationMs + elapsed)
            ActiveState.SLEEP -> currentRecord.copy(sleepDurationMs = currentRecord.sleepDurationMs + elapsed)
            else -> currentRecord
        }
        timeRecordDao.insertOrUpdate(updatedRecord)
    }
}
