package com.example.timetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    application: Application,
    private val repository: TrackerRepository = TrackerRepository(application)
) : AndroidViewModel(application) {

    val records: StateFlow<List<TimeRecord>> = repository.allRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
}
