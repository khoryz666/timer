package io.github.khoryz666.timer.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.khoryz666.timer.BuildConfig

@Composable
fun VersionInfo() {
    Text("Timer ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
}
