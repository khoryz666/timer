package com.example.timetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.example.timetracker.data.ActiveState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: TrackerUiState,
    isNotificationEnabled: Boolean,
    onNavigateToHistory: () -> Unit,
    onStateChange: (ActiveState) -> Unit,
    onResetCurrent: () -> Unit,
    onForceSave: () -> Unit,
    onToggleNotification: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Time Tracker") })
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
            
            // The Three Main Timer Buttons
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onResetCurrent) {
                    Text("Reset Current")
                }
                Button(onClick = onForceSave) {
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

            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onNavigateToHistory, modifier = Modifier.weight(1f)) {
                    Text("View History")
                }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/khoryz666/TimeTracker/releases/latest"))
                    context.startActivity(intent)
                }, modifier = Modifier.weight(1f)) {
                    Text("Check for Updates")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggles
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Notification")
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = onToggleNotification
                        )
                    }
                }
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
    
    val timeString = com.example.timetracker.util.TimeFormatter.formatHMS(totalMs)

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
