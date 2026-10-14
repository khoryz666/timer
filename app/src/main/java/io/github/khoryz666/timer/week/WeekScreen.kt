package io.github.khoryz666.timer.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.core.ui.pillarColor
import io.github.khoryz666.timer.quota.DaySummary
import io.github.khoryz666.timer.quota.ProjectSplit
import io.github.khoryz666.timer.tracking.Pillar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekRoute(contentPadding: PaddingValues) {
    val viewModel = appViewModel { WeekViewModel(it.database.sessions(), it.database.projects(), it.settings, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var openDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val current = state ?: return
    WeekScreen(
        state = current,
        contentPadding = contentPadding,
        onPrevious = viewModel::previousWeek,
        onNext = viewModel::nextWeek,
        onDayClick = { openDay = it },
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

private val RangeStart = DateTimeFormatter.ofPattern("d MMM")
private val DayName = DateTimeFormatter.ofPattern("EEE")
private val DayNumber = DateTimeFormatter.ofPattern("d")

@Composable
fun WeekScreen(
    state: WeekState,
    contentPadding: PaddingValues,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val summary = state.summary
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
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

        val engineColor = pillarColor(Pillar.ENGINE)
        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Engine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${summary.engineBlocks}", style = MaterialTheme.typography.headlineMedium.merge(Numeric), color = engineColor)
                Text(
                    " / ${summary.weeklyTargetBlocks} blocks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(summary.engineBlocks.toFloat() / summary.weeklyTargetBlocks, engineColor)
            Spacer(Modifier.height(8.dp))
            Text(
                "${formatDuration(summary.engineMs)} focused",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.split.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Project split")
                summary.split.forEach { ProjectSplitRow(it) }
            }
        }

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
}

@Composable
private fun ProjectSplitRow(split: ProjectSplit) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(split.project.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(formatPercent(split.actualShare), style = MaterialTheme.typography.titleMedium.merge(Numeric))
            Text(
                "  target ${formatPercent(split.targetShare)}",
                style = MaterialTheme.typography.bodySmall.merge(Numeric),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            split.actualShare.toFloat(),
            pillarColor(Pillar.ENGINE),
            marker = split.targetShare.toFloat().takeIf { it > 0f },
        )
    }
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
        BlockDots(day.engineBlocks)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatDuration(day.totals.engineMs),
                style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                color = pillarColor(Pillar.ENGINE),
            )
            Text(
                formatDuration(day.totals.joyMs),
                style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                color = pillarColor(Pillar.JOY),
            )
        }
    }
}

@Composable
private fun BlockDots(blocks: Int) {
    val color = pillarColor(Pillar.ENGINE)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(minOf(blocks, 6)) { Surface(Modifier.size(10.dp), shape = CircleShape, color = color) {} }
        Text(
            if (blocks == 1) "1 block" else "$blocks blocks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.widthIn(min = 56.dp).padding(start = 4.dp),
        )
    }
}
