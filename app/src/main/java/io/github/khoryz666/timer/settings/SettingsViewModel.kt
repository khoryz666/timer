package io.github.khoryz666.timer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.quota.QuotaSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val quota: QuotaSettings, val nudgeEnabled: Boolean, val projects: List<Project>) {
    val shareTotal: Int get() = projects.sumOf { it.sharePercent }
}

class SettingsViewModel(private val settings: SettingsStore, private val projects: ProjectDao) : ViewModel() {

    val state: StateFlow<SettingsState?> = combine(settings.quota, settings.nudgeEnabled, projects.observeAll()) { quota, nudge, all ->
        SettingsState(quota, nudge, all.filterNot { it.archived })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateQuota(transform: (QuotaSettings) -> QuotaSettings) = launch { settings.updateQuota(transform) }

    fun setNudgeEnabled(enabled: Boolean) = launch { settings.setNudgeEnabled(enabled) }

    fun addProject(name: String) = launch { projects.insert(Project(name = name.trim(), sharePercent = 0)) }

    fun renameProject(project: Project, name: String) = launch { projects.update(project.copy(name = name.trim())) }

    fun setShare(project: Project, percent: Int) = launch { projects.update(project.copy(sharePercent = percent.coerceIn(0, 100))) }

    /** Archives rather than deletes, so past sessions keep their project name. */
    fun removeProject(project: Project) = launch { projects.update(project.copy(archived = true)) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
