package io.github.khoryz666.timer.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.core.formatPercent
import io.github.khoryz666.timer.core.ui.Numeric
import io.github.khoryz666.timer.core.ui.ProgressBar
import io.github.khoryz666.timer.core.ui.SectionCard
import io.github.khoryz666.timer.core.ui.SectionLabel
import io.github.khoryz666.timer.core.ui.StepButtons
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.core.ui.pillarColor
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DaySummary
import io.github.khoryz666.timer.quota.ProjectSplit
import io.github.khoryz666.timer.quota.TrackWeek
import io.github.khoryz666.timer.tracking.Pillar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekRoute() {
    val viewModel = appViewModel {
        WeekViewModel(it.database.sessions(), it.database.projects(), it.database.dayTypes(), it.settings, it.clock)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var openDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val current = state ?: return
    WeekScreen(
        state = current,
        onPrevious = viewModel::previousWeek,
        onNext = viewModel::nextWeek,
        onDayClick = { openDay = it },
        projectEdits = ProjectEdits(
            add = viewModel::addProject,
            rename = viewModel::renameProject,
            setShare = viewModel::setShare,
            remove = viewModel::removeProject,
        ),
    )
    openDay?.let { day ->
        DaySheet(
            date = day,
            sessions = current.sessionsByDay[day].orEmpty(),
            projectNames = current.projectNames,
            now = current.now,
            onDelete = viewModel::delete,
            onDismiss = { openDay = null },
        )
    }
}

/** Edits to the current week's project plan. */
class ProjectEdits(
    val add: (String) -> Unit,
    val rename: (Project, String) -> Unit,
    val setShare: (Project, Int) -> Unit,
    val remove: (Project) -> Unit,
)

private val RangeStart = DateTimeFormatter.ofPattern("d MMM")
private val DayName = DateTimeFormatter.ofPattern("EEE")
private val DayNumber = DateTimeFormatter.ofPattern("d")

@Composable
fun WeekScreen(
    state: WeekState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    projectEdits: ProjectEdits,
) {
    val summary = state.summary
    var editing by rememberSaveable { mutableStateOf<ProjectNameEdit?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (state.isCurrentWeek) "This week" else "Week", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${summary.weekStart.format(RangeStart)} – ${summary.weekStart.plusDays(6).format(RangeStart)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            IconButton(onClick = onNext, enabled = !state.isCurrentWeek) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Focus", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "${formatDuration(summary.focusMs)} focused",
                    style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            summary.tracks.forEach { TrackRow(it) }
            Spacer(Modifier.height(8.dp))
            Text(
                "Weekly targets add up each day's targets, set by its day type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ProjectsCard(state, projectEdits, onRename = { editing = ProjectNameEdit(it.id, it.name) }, onAdd = { editing = ProjectNameEdit(null, "") })

        val joyColor = pillarColor(Pillar.JOY)
        val daysSoFar = if (state.isCurrentWeek) state.today.dayOfWeek.value else 7
        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Joy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(formatDuration(summary.joyMs), style = MaterialTheme.typography.headlineMedium.merge(Numeric), color = joyColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Average ${formatDuration(summary.joyMs / daysSoFar)} a day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            SectionLabel("Days")
            Spacer(Modifier.height(4.dp))
            summary.days.forEachIndexed { index, day ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DayRow(day, isFuture = day.date > state.today, isToday = day.date == state.today, onClick = { onDayClick(day.date) })
            }
        }
    }

    editing?.let { edit ->
        ProjectNameDialog(
            initial = edit.name,
            isNew = edit.projectId == null,
            onDismiss = { editing = null },
            onConfirm = { name ->
                val project = state.plan.find { it.id == edit.projectId }
                if (project == null) projectEdits.add(name) else projectEdits.rename(project, name)
                editing = null
            },
        )
    }
}

private data class ProjectNameEdit(val projectId: Long?, val name: String) : java.io.Serializable

@Composable
private fun TrackRow(track: TrackWeek) {
    val color = pillarColor(track.pillar)
    Column(Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(track.pillar.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${track.blocks}", style = MaterialTheme.typography.titleLarge.merge(Numeric), color = color)
            Text(
                " / ${track.targetBlocks} blocks",
                style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(if (track.targetBlocks == 0) 0f else track.blocks.toFloat() / track.targetBlocks, color)
    }
}

/** The week's project split; for the current week, also where its project list is edited. */
@Composable
private fun ProjectsCard(state: WeekState, edits: ProjectEdits, onRename: (Project) -> Unit, onAdd: () -> Unit) {
    val editable = state.isCurrentWeek
    val planned = state.plan.map { it.id }.toSet()
    SectionCard {
        SectionLabel(if (editable) "Projects this week" else "Projects")
        state.summary.split.forEach { split ->
            ProjectSplitRow(
                split = split,
                editable = editable && split.project.id in planned,
                canRemove = state.plan.size > 1,
                onRename = { onRename(split.project) },
                onShareChange = { edits.setShare(split.project, it) },
                onRemove = { edits.remove(split.project) },
            )
        }
        if (editable) {
            if (state.shareTotal != 100) {
                Text(
                    "Shares add up to ${state.shareTotal}%; targets are scaled to 100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add project", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "Changes apply from this week on; earlier weeks keep their own list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectSplitRow(
    split: ProjectSplit,
    editable: Boolean,
    canRemove: Boolean,
    onRename: () -> Unit,
    onShareChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val project = split.project
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(enabled = editable, onClick = onRename).padding(vertical = 4.dp)) {
                Text(project.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${formatPercent(split.actualShare)} · target ${formatPercent(split.targetShare)}",
                    style = MaterialTheme.typography.bodySmall.merge(Numeric),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (editable) {
                StepButtons(
                    "${project.sharePercent}%",
                    onMinus = { onShareChange(project.sharePercent - 5) }.takeIf { project.sharePercent > 0 },
                    onPlus = { onShareChange(project.sharePercent + 5) }.takeIf { project.sharePercent < 100 },
                    minValueWidth = 52,
                )
                IconButton(onClick = onRemove, enabled = canRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove ${project.name} from this week")
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            split.actualShare.toFloat(),
            pillarColor(Pillar.PROJECT),
            marker = split.targetShare.toFloat().takeIf { it > 0f },
        )
    }
}

@Composable
private fun ProjectNameDialog(initial: String, isNew: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New project this week" else "Rename project") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(if (isNew) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DayRow(day: DaySummary, isFuture: Boolean, isToday: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFuture, onClick = onClick)
            .alpha(if (isFuture) 0.4f else 1f)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.date.format(DayName),
                style = MaterialTheme.typography.labelMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(day.date.format(DayNumber), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(day.dayType.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pillar.FOCUS.forEach { pillar ->
                    Text(
                        "${pillar.shortLabel} ${day.blocks.getValue(pillar)}/${day.targets.getValue(pillar)}",
                        style = MaterialTheme.typography.bodySmall.merge(Numeric),
                        color = pillarColor(pillar),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            LabelledTime("Focus", day.totals.focusMs, Pillar.PROJECT)
            LabelledTime("Joy", day.totals.joyMs, Pillar.JOY)
        }
    }
}

@Composable
private fun LabelledTime(label: String, ms: Long, pillar: Pillar) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatDuration(ms),
            style = MaterialTheme.typography.bodyMedium.merge(Numeric),
            color = pillarColor(pillar),
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 64.dp),
        )
    }
}
