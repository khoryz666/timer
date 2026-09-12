package com.example.timetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.TrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    private val repository = TrackerRepository(application)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Collect state from preferences and local DB
        viewModelScope.launch {
            combine(
                repository.activeStateFlow,
                repository.startTimeFlow,
                repository.allRecordsFlow()
            ) { state, startTime, records ->
                val dateStr = getCurrentDateString()
                val currentRecord = records.find { it.date == dateStr }
                Triple(state, startTime, currentRecord)
            }.collect { (state, startTime, record) ->
                _uiState.value = TrackerUiState(
                    activeState = state,
                    workDurationMs = record?.workDurationMs ?: 0L,
                    selfDurationMs = record?.selfDurationMs ?: 0L,
                    sleepDurationMs = record?.sleepDurationMs ?: 0L,
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
                val state = repository.activeStateFlow.first()
                if (state != ActiveState.IDLE) {
                    val startTime = repository.startTimeFlow.first()
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
            repository.switchState(newState)
        }
    }

    fun resetActiveTimer() {
        viewModelScope.launch {
            repository.resetActiveTimer()
        }
    }

    fun forceSaveCurrentProgress() {
        viewModelScope.launch {
            repository.forceSaveCurrentProgress()
        }
    }
}
