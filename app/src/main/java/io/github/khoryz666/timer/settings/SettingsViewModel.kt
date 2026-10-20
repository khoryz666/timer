package io.github.khoryz666.timer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.day.DayPromptSettings
import io.github.khoryz666.timer.quota.QuotaSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val quota: QuotaSettings, val nudgeEnabled: Boolean, val dayPrompt: DayPromptSettings)

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    val state: StateFlow<SettingsState?> = combine(settings.quota, settings.nudgeEnabled, settings.dayPrompt, ::SettingsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateQuota(transform: (QuotaSettings) -> QuotaSettings) = launch { settings.updateQuota(transform) }

    fun setNudgeEnabled(enabled: Boolean) = launch { settings.setNudgeEnabled(enabled) }

    fun setDayPrompt(prompt: DayPromptSettings) = launch { settings.setDayPrompt(prompt) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
