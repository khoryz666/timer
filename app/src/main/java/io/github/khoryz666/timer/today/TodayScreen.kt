package io.github.khoryz666.timer.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.core.formatElapsed
import io.github.khoryz666.timer.core.ui.BlockSegments
import io.github.khoryz666.timer.core.ui.Numeric
import io.github.khoryz666.timer.core.ui.ProgressBar
import io.github.khoryz666.timer.core.ui.SectionCard
import io.github.khoryz666.timer.core.ui.SectionLabel
import io.github.khoryz666.timer.core.ui.StatusChip
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.core.ui.onPillarColor
import io.github.khoryz666.timer.core.ui.pillarColor
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.QuotaStatus
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.TodayStatus
import io.github.khoryz666.timer.tracking.TrackTarget
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TodayRoute() {
    val viewModel = appViewModel { TodayViewModel(it.tracker) }
    val status by viewModel.status.collectAsStateWithLifecycle()
    status?.let {
        TodayScreen(it, onSwitch = viewModel::switchTo, onStop = viewModel::stop, onDayType = viewModel::setDayType)
    }
}

@Composable
fun TodayScreen(status: TodayStatus, onSwitch: (TrackTarget) -> Unit, onStop: () -> Unit, onDayType: (DayType) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(status)
        DayTypeCard(status, onDayType)
        NowCard(status, onStop)
        ProjectCard(status, onSwitch)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallTrackCard(status, Pillar.NEETCODE, TrackTarget.NeetCode, onSwitch, Modifier.weight(1f).fillMaxHeight())
            SmallTrackCard(status, Pillar.READING, TrackTarget.Reading, onSwitch, Modifier.weight(1f).fillMaxHeight())
        }
        JoyCard(status, onSwitch)
    }
}

private val DayFormat = DateTimeFormatter.ofPattern("EEEE")
private val DateFormat = DateTimeFormatter.ofPattern("d MMMM")

@Composable
private fun Header(status: TodayStatus) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(status.date.format(DayFormat), style = MaterialTheme.typography.headlineSmall)
        Text(status.date.format(DateFormat), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A three-stop slider, Light - Class - Deep, that sets today's targets. */
@Composable
private fun DayTypeCard(status: TodayStatus, onDayType: (DayType) -> Unit) {
    var position by remember(status.dayType) { mutableFloatStateOf(status.dayType.ordinal.toFloat()) }
    val shown = DayType.entries[position.roundToInt()]
    SectionCard {
        CardHeader("${shown.label} day") {
            if (!status.dayTypeChosen) {
                TextButton(onClick = { onDayType(status.dayType) }) { Text("Keep ${status.dayType.label}") }
            }
        }
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = { onDayType(DayType.entries[position.roundToInt()]) },
            valueRange = 0f..(DayType.entries.size - 1).toFloat(),
            steps = DayType.entries.size - 2,
            // Neutral, so the day type doesn't borrow Joy's colour for the unselected part.
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.semantics { contentDescription = "Day type" },
        )
        Row(Modifier.fillMaxWidth()) {
            DayType.entries.forEachIndexed { index, type ->
                Text(
                    type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (type == shown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        DayType.entries.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val targets = status.targets
        Text(
            if (status.dayTypeChosen) {
                "Targets: Project ${targets.project} · NeetCode ${targets.neetCode} · Reading ${targets.reading} blocks"
            } else {
                "Not picked yet, so today uses your last choice."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NowCard(status: TodayStatus, onStop: () -> Unit) {
    val running = status.running
    val color = running?.let { pillarColor(it.pillar) }
    SectionCard(accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(8.dp), shape = CircleShape, color = color ?: MaterialTheme.colorScheme.outline) {}
                    Spacer(Modifier.width(8.dp))
                    SectionLabel(
                        when (running?.pillar) {
                            Pillar.PROJECT -> "Project · ${status.runningProjectName}"
                            null -> "Not tracking"
                            else -> running.pillar.label
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (running != null) {
                    Text(formatElapsed(running.durationMs(status.now)), style = MaterialTheme.typography.displayMedium)
                } else {
                    Text(
                        "Tap a project, NeetCode, Reading or Joy to start.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (running != null) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun ProjectCard(status: TodayStatus, onSwitch: (TrackTarget) -> Unit) {
    val track = status.track(Pillar.PROJECT)
    val color = pillarColor(Pillar.PROJECT)
    SectionCard(accent = if (status.running?.pillar == Pillar.PROJECT) color else null) {
        CardHeader(Pillar.PROJECT.label) { StatusChip(track.status, hasFloor = track.floorBlocks > 0) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${track.blocks}", style = MaterialTheme.typography.displayMedium, color = color)
            Text(
                " / ${track.targetBlocks} blocks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${formatDuration(track.trackedMs)} of ${formatDuration(track.targetMs)}",
                style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        BlockSegments(track.blocks, track.nextBlockFraction, track.targetBlocks, track.floorBlocks, color)
        Spacer(Modifier.height(6.dp))
        Text(
            "Floor ${track.floorBlocks} · target ${track.targetBlocks}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        status.projects.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { project ->
                    ProjectButton(
                        project = project,
                        trackedMs = status.totals.byProject[project.id] ?: 0,
                        running = status.running?.target == TrackTarget.Project(project.id),
                        onClick = { onSwitch(TrackTarget.Project(project.id)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectButton(project: Project, trackedMs: Long, running: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = pillarColor(Pillar.PROJECT)
    Surface(
        onClick = onClick,
        enabled = !running,
        modifier = modifier.heightIn(min = 64.dp).semantics { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        color = if (running) color else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (running) onPillarColor(Pillar.PROJECT) else MaterialTheme.colorScheme.onSurface,
        border = if (running) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    if (running) "Running · ${formatDuration(trackedMs)}" else formatDuration(trackedMs),
                    style = MaterialTheme.typography.bodySmall.merge(Numeric),
                )
            }
            if (!running) Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = color)
        }
    }
}

/** NeetCode or Reading: half-width, with its blocks and a start button. */
@Composable
private fun SmallTrackCard(
    status: TodayStatus,
    pillar: Pillar,
    target: TrackTarget,
    onSwitch: (TrackTarget) -> Unit,
    modifier: Modifier,
) {
    val track = status.track(pillar)
    val color = pillarColor(pillar)
    val running = status.running?.pillar == pillar
    SectionCard(modifier, accent = if (running) color else null) {
        Text(pillar.label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${track.blocks}", style = MaterialTheme.typography.displayMedium, color = color)
            Text(
                if (track.hasTarget) " / ${track.targetBlocks}" else " blocks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        BlockSegments(track.blocks, track.nextBlockFraction, track.targetBlocks, track.floorBlocks, color)
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                !track.hasTarget -> "No target today"
                track.status == QuotaStatus.TARGET_MET -> "Target met"
                else -> "${formatDuration(track.remainingMs)} to go"
            },
            style = MaterialTheme.typography.labelSmall.merge(Numeric),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        FilledTonalButton(
            onClick = { onSwitch(target) },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = color,
                contentColor = onPillarColor(pillar),
            ),
        ) {
            Text(if (running) "Running" else "Start ${pillar.label}", maxLines = 1)
        }
    }
}

@Composable
private fun JoyCard(status: TodayStatus, onSwitch: (TrackTarget) -> Unit) {
    val joy = status.joy
    val color = pillarColor(Pillar.JOY)
    val running = status.running?.pillar == Pillar.JOY
    SectionCard(accent = if (running) color else null) {
        CardHeader("Joy") { StatusChip(joy.status, hasFloor = false) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatDuration(joy.trackedMs), style = MaterialTheme.typography.displayMedium, color = color)
            Text(
                " of ${formatDuration(joy.targetMs)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(joy.fraction, color)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = { onSwitch(TrackTarget.Joy) },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            if (!running) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (running) "Joy is running" else "Start Joy")
        }
    }
}

@Composable
private fun CardHeader(title: String, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}
