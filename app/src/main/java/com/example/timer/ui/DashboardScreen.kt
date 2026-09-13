package com.example.timer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timer.data.ActiveState
import com.example.timer.data.TrackerSnapshot
import com.example.timer.util.TimeFormatter

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
                title = {
                    Text(
                        "Timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // The three main timer cards - the primary, most frequent actions
            TimerButton(
                title = "Work",
                state = ActiveState.WORK,
                currentState = uiState.activeState,
                baseDurationMs = uiState.workDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.WORK) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            TimerButton(
                title = "Self",
                state = ActiveState.SELF,
                currentState = uiState.activeState,
                baseDurationMs = uiState.selfDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.SELF) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            TimerButton(
                title = "Sleep",
                state = ActiveState.SLEEP,
                currentState = uiState.activeState,
                baseDurationMs = uiState.sleepDurationMs,
                activeTickingMs = uiState.activelyTickingMs,
                onClick = { onStateChange(ActiveState.SLEEP) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Secondary, less-frequent actions - tonal so they don't compete with the
            // timer cards above or the stop action below.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(onClick = onResetCurrent, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
                FilledTonalButton(onClick = onForceSave, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onStateChange(ActiveState.IDLE) },
                enabled = uiState.activeState != ActiveState.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop All Timers", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "timerCardContainer"
    )
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 0.dp),
        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(state = state, size = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = timeString, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
