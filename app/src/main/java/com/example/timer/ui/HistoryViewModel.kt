package com.example.timer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timer.data.TimeRecord
import com.example.timer.data.TrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TrackerRepository = TrackerRepository(application)
) : AndroidViewModel(application) {

    val records: StateFlow<List<TimeRecord>> = repository.allRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
}
