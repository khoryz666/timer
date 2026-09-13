package com.example.timetracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.TrackerSnapshot
import com.example.timetracker.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: TrackerSnapshot,
    onStateChange: (ActiveState) -> Unit,
    onResetCurrent: () -> Unit,
    onForceSave: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Tracker") },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/khoryz666/timer/releases/latest")
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Check for updates")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The Three Main Timer Buttons - the primary, most frequent actions
            TimerButton(
                title = "Work",
                state = ActiveState.WORK,
                currentState = uiState.activeState,
                baseDurationMs = uiState.workDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.WORK) }
            )

            TimerButton(
                title = "Self",
                state = ActiveState.SELF,
                currentState = uiState.activeState,
                baseDurationMs = uiState.selfDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.SELF) }
            )

            TimerButton(
                title = "Sleep",
                state = ActiveState.SLEEP,
                currentState = uiState.activeState,
                baseDurationMs = uiState.sleepDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.SLEEP) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary, less-frequent actions - outlined so they don't compete with the
            // primary timer buttons above or the stop action below.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onResetCurrent) {
                    Text("Reset Current")
                }
                OutlinedButton(onClick = onForceSave) {
                    Text("Save Progress")
                }
            }

            Button(
                onClick = { onStateChange(ActiveState.IDLE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Stop All Timers")
            }
        }
    }
}

@Composable
fun TimerButton(
    title: String,
    state: ActiveState,
    currentState: ActiveState,
    baseDurationMs: Long,
    activeTickingMs: Long,
    onClick: () -> Unit
) {
    val isActive = state == currentState
    val totalMs = baseDurationMs + if (isActive) activeTickingMs else 0L

    val timeString = TimeFormatter.formatHMS(totalMs)

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = timeString, fontSize = 32.sp)
        }
    }
}
