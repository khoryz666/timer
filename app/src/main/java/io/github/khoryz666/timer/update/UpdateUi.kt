package io.github.khoryz666.timer.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.khoryz666.timer.core.ui.SectionLabel

/** The Settings card: installed version, and a manual check with a clear result. */
@Composable
fun ColumnScope.UpdatesSection(state: UpdateState, onCheck: () -> Unit, onOpenUpdate: () -> Unit) {
    SectionLabel("Updates")
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Timer ${state.installedVersion}", style = MaterialTheme.typography.bodyLarge)
            val status = when {
                !state.enabled -> "Update checks are off in debug builds."
                state.phase == UpdatePhase.Checking -> "Checking…"
                state.available != null -> "Version ${state.available.version} is available."
                else -> state.message
            }
            if (status != null) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.available != null) {
            Button(onClick = onOpenUpdate) { Text("Update") }
        } else {
            OutlinedButton(onClick = onCheck, enabled = state.enabled && state.phase == UpdatePhase.Idle) { Text("Check") }
        }
    }
}

@Composable
fun UpdateDialog(viewModel: UpdateViewModel, state: UpdateState) {
    val release = state.available ?: return
    if (!state.dialogOpen) return
    val context = LocalContext.current
    val phase = state.phase
    val busy = phase is UpdatePhase.Downloading || phase == UpdatePhase.Installing

    AlertDialog(
        onDismissRequest = { if (!busy) viewModel.dismiss() },
        title = { Text("Update to ${release.version}") },
        text = {
            Column {
                when (phase) {
                    UpdatePhase.NeedsPermission -> Text(
                        "To install updates, Timer needs permission to install apps. " +
                            "Turn on \"Allow from this source\" on the next screen, then come back.",
                    )
                    is UpdatePhase.Downloading -> {
                        Text("Downloading…")
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { phase.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    UpdatePhase.Installing -> Text("Confirm the installation in the system dialog.")
                    else -> Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        Text(release.notes.ifBlank { "No release notes." }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            when (phase) {
                UpdatePhase.NeedsPermission ->
                    TextButton(onClick = { context.startActivity(viewModel.permissionSettingsIntent()) }) { Text("Open settings") }
                UpdatePhase.Idle, UpdatePhase.Checking -> TextButton(onClick = viewModel::install) { Text("Update") }
                else -> Unit
            }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(if (busy) "Cancel" else "Later") } },
    )
}
