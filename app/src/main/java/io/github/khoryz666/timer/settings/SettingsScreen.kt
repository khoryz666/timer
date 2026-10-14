package io.github.khoryz666.timer.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.core.minutesToMs
import io.github.khoryz666.timer.core.ui.Numeric
import io.github.khoryz666.timer.core.ui.SectionCard
import io.github.khoryz666.timer.core.ui.SectionLabel
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.QuotaSettings

@Composable
fun SettingsRoute(updates: @Composable ColumnScope.() -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.database.projects()) }
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
    var editing by rememberSaveable { mutableStateOf<ProjectEdit?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))

        SectionCard {
            SectionLabel("Engine")
            Stepper(
                "Block length",
                "${quota.blockMinutes} min",
                onMinus = { viewModel.updateQuota { it.copy(blockMinutes = it.blockMinutes - 15) } }.takeIf { quota.blockMinutes > QuotaSettings.BLOCK_MINUTES.first },
                onPlus = { viewModel.updateQuota { it.copy(blockMinutes = it.blockMinutes + 15) } }.takeIf { quota.blockMinutes < QuotaSettings.BLOCK_MINUTES.last },
            )
            Stepper(
                "Daily floor",
                blocks(quota.floorBlocks),
                onMinus = { viewModel.updateQuota { it.copy(floorBlocks = it.floorBlocks - 1) } }.takeIf { quota.floorBlocks > 0 },
                onPlus = { viewModel.updateQuota { it.copy(floorBlocks = it.floorBlocks + 1) } }.takeIf { quota.floorBlocks < quota.targetBlocks },
            )
            Stepper(
                "Daily target",
                blocks(quota.targetBlocks),
                supporting = formatDuration(quota.engineTargetMs) + " a day",
                onMinus = { viewModel.updateQuota { it.copy(targetBlocks = it.targetBlocks - 1) } }.takeIf { quota.targetBlocks > QuotaSettings.TARGET_BLOCKS.first },
                onPlus = { viewModel.updateQuota { it.copy(targetBlocks = it.targetBlocks + 1) } }.takeIf { quota.targetBlocks < QuotaSettings.TARGET_BLOCKS.last },
            )
            Stepper(
                "Weekly target",
                blocks(quota.weeklyTargetBlocks),
                onMinus = { viewModel.updateQuota { it.copy(weeklyTargetBlocks = it.weeklyTargetBlocks - 1) } }.takeIf { quota.weeklyTargetBlocks > QuotaSettings.WEEKLY_TARGET_BLOCKS.first },
                onPlus = { viewModel.updateQuota { it.copy(weeklyTargetBlocks = it.weeklyTargetBlocks + 1) } }.takeIf { quota.weeklyTargetBlocks < QuotaSettings.WEEKLY_TARGET_BLOCKS.last },
            )
        }

        SectionCard {
            SectionLabel("Joy")
            Stepper(
                "Daily target",
                formatDuration(minutesToMs(quota.joyTargetMinutes)),
                onMinus = { viewModel.updateQuota { it.copy(joyTargetMinutes = it.joyTargetMinutes - 15) } }.takeIf { quota.joyTargetMinutes > QuotaSettings.JOY_TARGET_MINUTES.first },
                onPlus = { viewModel.updateQuota { it.copy(joyTargetMinutes = it.joyTargetMinutes + 15) } }.takeIf { quota.joyTargetMinutes < QuotaSettings.JOY_TARGET_MINUTES.last },
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nudge", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A gentle notice if Joy starts before the day's first Engine block. Never blocks anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.nudgeEnabled, onCheckedChange = viewModel::setNudgeEnabled, modifier = Modifier.padding(start = 12.dp))
            }
        }

        SectionCard {
            SectionLabel("Projects · weekly split")
            state.projects.forEach { project ->
                ProjectRow(
                    project = project,
                    canRemove = state.projects.size > 1,
                    onRename = { editing = ProjectEdit(project.id, project.name) },
                    onShareChange = { viewModel.setShare(project, it) },
                    onRemove = { viewModel.removeProject(project) },
                )
            }
            if (state.shareTotal != 100) {
                Text(
                    "Shares add up to ${state.shareTotal}%; targets are scaled to 100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(onClick = { editing = ProjectEdit(null, "") }, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add project", modifier = Modifier.padding(start = 8.dp))
            }
        }

        SectionCard(content = updates)
    }

    editing?.let { edit ->
        ProjectNameDialog(
            initial = edit.name,
            isNew = edit.projectId == null,
            onDismiss = { editing = null },
            onConfirm = { name ->
                val project = state.projects.find { it.id == edit.projectId }
                if (project == null) viewModel.addProject(name) else viewModel.renameProject(project, name)
                editing = null
            },
        )
    }
}

private data class ProjectEdit(val projectId: Long?, val name: String) : java.io.Serializable

private fun blocks(count: Int) = if (count == 1) "1 block" else "$count blocks"

/** A labelled value with -/+ buttons; a null callback disables that button. */
@Composable
private fun Stepper(label: String, value: String, onMinus: (() -> Unit)?, onPlus: (() -> Unit)?, supporting: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StepButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.merge(Numeric),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 84.dp),
        )
        StepButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

/** A neutral -/+ button, so settings don't borrow a pillar colour. */
@Composable
private fun StepButton(onClick: () -> Unit, enabled: Boolean, content: @Composable () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        content = content,
    )
}

@Composable
private fun ProjectRow(project: Project, canRemove: Boolean, onRename: () -> Unit, onShareChange: (Int) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(onClick = onRename).padding(vertical = 8.dp)) {
            Text(project.name, style = MaterialTheme.typography.bodyLarge)
            Text("Tap to rename", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StepButton(onClick = { onShareChange(project.sharePercent - 5) }, enabled = project.sharePercent > 0) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "${project.sharePercent}%",
            style = MaterialTheme.typography.bodyLarge.merge(Numeric),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 52.dp),
        )
        StepButton(onClick = { onShareChange(project.sharePercent + 5) }, enabled = project.sharePercent < 100) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove ${project.name}")
        }
    }
}

@Composable
private fun ProjectNameDialog(initial: String, isNew: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New project" else "Rename project") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(if (isNew) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
