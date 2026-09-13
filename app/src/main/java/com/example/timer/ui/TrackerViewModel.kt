package com.example.timer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timer.data.ActiveState
import com.example.timer.data.TrackerRepository
import com.example.timer.data.TrackerSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(
    application: Application,
    private val repository: TrackerRepository = TrackerRepository(application)
) : AndroidViewModel(application) {

    val uiState: StateFlow<TrackerSnapshot> = repository.snapshotFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TrackerSnapshot())

    fun onButtonPress(newState: ActiveState) {
        viewModelScope.launch { repository.switchState(newState) }
    }

    fun resetActiveTimer() {
        viewModelScope.launch { repository.resetActiveTimer() }
    }

    fun forceSaveCurrentProgress() {
        viewModelScope.launch { repository.forceSaveCurrentProgress() }
    }
}
