package io.github.khoryz666.timer.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.core.minutesToMs
import io.github.khoryz666.timer.core.ui.SectionCard
import io.github.khoryz666.timer.core.ui.SectionLabel
import io.github.khoryz666.timer.core.ui.Stepper
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.core.ui.blocksLabel
import io.github.khoryz666.timer.day.DayPromptSettings
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.quota.DayTargets
import io.github.khoryz666.timer.quota.QuotaSettings

@Composable
fun SettingsRoute(updates: @Composable ColumnScope.() -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { SettingsScreen(it, viewModel, updates) }
}

@Composable
private fun SettingsScreen(
    state: SettingsState,
    viewModel: SettingsViewModel,
    updates: @Composable ColumnScope.() -> Unit,
) {
    val quota = state.quota

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))

        SectionCard {
            SectionLabel("Blocks")
            Stepper(
                "Block length",
                "${quota.blockMinutes} min",
                supporting = "Project, NeetCode and Reading are counted in whole blocks",
                onMinus = { viewModel.updateQuota { it.copy(blockMinutes = it.blockMinutes - 15) } }.takeIf { quota.blockMinutes > QuotaSettings.BLOCK_MINUTES.first },
                onPlus = { viewModel.updateQuota { it.copy(blockMinutes = it.blockMinutes + 15) } }.takeIf { quota.blockMinutes < QuotaSettings.BLOCK_MINUTES.last },
            )
        }

        DayTypesCard(quota, viewModel)

        SectionCard {
            SectionLabel("Joy")
            Stepper(
                "Daily target",
                formatDuration(minutesToMs(quota.joyTargetMinutes)),
                onMinus = { viewModel.updateQuota { it.copy(joyTargetMinutes = it.joyTargetMinutes - 15) } }.takeIf { quota.joyTargetMinutes > QuotaSettings.JOY_TARGET_MINUTES.first },
                onPlus = { viewModel.updateQuota { it.copy(joyTargetMinutes = it.joyTargetMinutes + 15) } }.takeIf { quota.joyTargetMinutes < QuotaSettings.JOY_TARGET_MINUTES.last },
            )
            SwitchRow(
                "Nudge",
                "A gentle notice if Joy starts before the day's first focus block. Never blocks anything.",
                checked = state.nudgeEnabled,
                onCheckedChange = viewModel::setNudgeEnabled,
            )
        }

        SectionCard {
            val prompt = state.dayPrompt
            SectionLabel("Daily day-type prompt")
            SwitchRow(
                "Ask every day",
                "A notification asks whether today is a Light, Class or Deep day, until you pick one.",
                checked = prompt.enabled,
                onCheckedChange = { viewModel.setDayPrompt(prompt.copy(enabled = it)) },
            )
            if (prompt.enabled) {
                val step = DayPromptSettings.STEP_MINUTES
                Stepper(
                    "Ask from",
                    "%02d:%02d".format(prompt.time.hour, prompt.time.minute),
                    onMinus = { viewModel.setDayPrompt(prompt.copy(minuteOfDay = prompt.minuteOfDay - step)) }
                        .takeIf { prompt.minuteOfDay > DayPromptSettings.MINUTE_OF_DAY.first },
                    onPlus = { viewModel.setDayPrompt(prompt.copy(minuteOfDay = prompt.minuteOfDay + step)) }
                        .takeIf { prompt.minuteOfDay < DayPromptSettings.MINUTE_OF_DAY.last },
                )
            }
        }

        SectionCard {
            SectionLabel("Projects")
            Text(
                "Projects and their shares are planned per week, on the Week screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionCard(content = updates)
    }
}

/** Each day type's targets, one day type at a time. */
@Composable
private fun DayTypesCard(quota: QuotaSettings, viewModel: SettingsViewModel) {
    var selected by rememberSaveable { mutableStateOf(DayType.DEEP) }
    val targets = quota.targets(selected)
    fun update(transform: (DayTargets) -> DayTargets) = viewModel.updateQuota { it.withTargets(selected, transform) }
    val max = QuotaSettings.TRACK_BLOCKS.last

    SectionCard {
        SectionLabel("Day types")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            DayType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = type == selected,
                    onClick = { selected = type },
                    shape = SegmentedButtonDefaults.itemShape(index, DayType.entries.size),
                ) { Text(type.label) }
            }
        }
        Stepper(
            "Project target",
            blocksLabel(targets.project),
            supporting = formatDuration(targets.project * quota.blockMs),
            onMinus = { update { it.copy(project = it.project - 1) } }.takeIf { targets.project > 0 },
            onPlus = { update { it.copy(project = it.project + 1) } }.takeIf { targets.project < max },
        )
        Stepper(
            "Project floor",
            blocksLabel(targets.projectFloor),
            onMinus = { update { it.copy(projectFloor = it.projectFloor - 1) } }.takeIf { targets.projectFloor > 0 },
            onPlus = { update { it.copy(projectFloor = it.projectFloor + 1) } }.takeIf { targets.projectFloor < targets.project },
        )
        Stepper(
            "NeetCode target",
            blocksLabel(targets.neetCode),
            onMinus = { update { it.copy(neetCode = it.neetCode - 1) } }.takeIf { targets.neetCode > 0 },
            onPlus = { update { it.copy(neetCode = it.neetCode + 1) } }.takeIf { targets.neetCode < max },
        )
        Stepper(
            "Reading target",
            blocksLabel(targets.reading),
            onMinus = { update { it.copy(reading = it.reading - 1) } }.takeIf { targets.reading > 0 },
            onPlus = { update { it.copy(reading = it.reading + 1) } }.takeIf { targets.reading < max },
        )
    }
}

@Composable
private fun SwitchRow(label: String, supporting: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.padding(start = 12.dp))
    }
}
