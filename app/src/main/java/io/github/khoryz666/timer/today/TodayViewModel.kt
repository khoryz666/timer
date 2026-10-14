package io.github.khoryz666.timer.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.tracking.TodayStatus
import io.github.khoryz666.timer.tracking.TrackTarget
import io.github.khoryz666.timer.tracking.Tracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(private val tracker: Tracker) : ViewModel() {

    val status: StateFlow<TodayStatus?> = tracker.today(tickMs = 1_000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun switchTo(target: TrackTarget) {
        viewModelScope.launch { tracker.switchTo(target) }
    }

    fun stop() {
        viewModelScope.launch { tracker.stop() }
    }
}
