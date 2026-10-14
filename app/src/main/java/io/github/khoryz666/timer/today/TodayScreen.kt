package io.github.khoryz666.timer.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import io.github.khoryz666.timer.core.ui.pillarColor
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.TodayStatus
import io.github.khoryz666.timer.tracking.TrackTarget
import java.time.format.DateTimeFormatter

@Composable
fun TodayRoute(contentPadding: PaddingValues) {
    val viewModel = appViewModel { TodayViewModel(it.tracker) }
    val status by viewModel.status.collectAsStateWithLifecycle()
    status?.let { TodayScreen(it, contentPadding, onSwitch = viewModel::switchTo, onStop = viewModel::stop) }
}

@Composable
fun TodayScreen(status: TodayStatus, contentPadding: PaddingValues, onSwitch: (TrackTarget) -> Unit, onStop: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(status)
        NowCard(status, onStop)
        EngineCard(status, onSwitch)
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
                            Pillar.ENGINE -> "Engine · ${status.runningProjectName}"
                            Pillar.JOY -> "Joy"
                            null -> "Not tracking"
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (running != null) {
                    Text(formatElapsed(running.durationMs(status.now)), style = MaterialTheme.typography.displayMedium)
                } else {
                    Text(
                        "Tap a project or Joy to start.",
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
private fun EngineCard(status: TodayStatus, onSwitch: (TrackTarget) -> Unit) {
    val engine = status.engine
    val color = pillarColor(Pillar.ENGINE)
    SectionCard(accent = if (status.running?.pillar == Pillar.ENGINE) color else null) {
        CardHeader("Engine") { StatusChip(engine.status, hasFloor = engine.floorBlocks > 0) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${engine.blocks}", style = MaterialTheme.typography.displayMedium, color = color)
            Text(
                " / ${engine.targetBlocks} blocks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${formatDuration(engine.trackedMs)} of ${formatDuration(engine.targetMs)}",
                style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        BlockSegments(engine.blocks, engine.nextBlockFraction, engine.targetBlocks, engine.floorBlocks, color)
        Spacer(Modifier.height(6.dp))
        Text(
            "Floor ${engine.floorBlocks} · target ${engine.targetBlocks}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        status.projects.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { project ->
                    ProjectButton(
                        project = project,
                        trackedMs = status.totals.engineByProject[project.id] ?: 0,
                        running = status.running?.target == TrackTarget.Engine(project.id),
                        color = color,
                        onClick = { onSwitch(TrackTarget.Engine(project.id)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectButton(project: Project, trackedMs: Long, running: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        enabled = !running,
        modifier = modifier.heightIn(min = 64.dp).semantics { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        color = if (running) color else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (running) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
