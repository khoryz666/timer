package io.github.khoryz666.timer.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.khoryz666.timer.appGraph
import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.core.ui.Numeric
import io.github.khoryz666.timer.core.ui.pillarColor
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SheetTitle = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val Time = DateTimeFormatter.ofPattern("HH:mm")
private val DayAndTime = DateTimeFormatter.ofPattern("EEE HH:mm")

/** The sessions that count toward [date], with a way to delete a mistaken one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    date: LocalDate,
    sessions: List<Session>,
    projectNames: Map<Long, String>,
    now: Long,
    onDelete: (Session) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = LocalContext.current.appGraph.clock.zone()
    var confirmDelete by remember { mutableStateOf<Session?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 24.dp, end = 12.dp, bottom = 32.dp)) {
            Text(date.format(SheetTitle), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (sessions.isEmpty()) {
                Text(
                    "Nothing tracked.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            sessions.forEach { session ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(10.dp), shape = CircleShape, color = pillarColor(session.pillar)) {}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (session.pillar == Pillar.JOY) "Joy" else projectNames[session.projectId] ?: "Engine",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            timeRange(session, date, zone),
                            style = MaterialTheme.typography.bodySmall.merge(Numeric),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(formatDuration(session.durationMs(now)), style = MaterialTheme.typography.bodyMedium.merge(Numeric))
                    if (session.isRunning) {
                        Spacer(Modifier.width(48.dp))
                    } else {
                        IconButton(onClick = { confirmDelete = session }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete session")
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this session?") },
            text = { Text("${formatDuration(session.durationMs(now))} will be removed from this day's totals.") },
            confirmButton = {
                TextButton(onClick = { onDelete(session); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

/** "09:00 – 10:30", "Mon 23:10 – 01:00" when it started the day before, "13:00 – now" while running. */
private fun timeRange(session: Session, date: LocalDate, zone: ZoneId): String {
    val start = Instant.ofEpochMilli(session.startedAt).atZone(zone)
    val startText = if (start.toLocalDate() == date) start.format(Time) else start.format(DayAndTime)
    val endText = session.endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).format(Time) } ?: "now"
    return "$startText – $endText"
}
