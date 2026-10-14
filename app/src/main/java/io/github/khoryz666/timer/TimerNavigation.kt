package io.github.khoryz666.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khoryz666.timer.core.ui.appViewModel
import io.github.khoryz666.timer.settings.SettingsRoute
import io.github.khoryz666.timer.today.TodayRoute
import io.github.khoryz666.timer.update.UpdateDialog
import io.github.khoryz666.timer.update.UpdateViewModel
import io.github.khoryz666.timer.update.UpdatesSection
import io.github.khoryz666.timer.week.WeekRoute

private enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.Home),
    Week("Week", Icons.Filled.DateRange),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun TimerNavigation() {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val updates = appViewModel {
        UpdateViewModel(
            it.releases,
            it.installer,
            it.updateChecks,
            it.clock,
            installedVersion = BuildConfig.VERSION_NAME,
            enabled = !BuildConfig.DEBUG,
        )
    }
    val updateState by updates.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updates.autoCheck() }
    LifecycleResumeEffect(Unit) {
        updates.onResume()
        onPauseOrDispose {}
    }
    UpdateDialog(updates, updateState)

    BackHandler(enabled = tab != Tab.Today) { tab = Tab.Today }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (tab) {
                Tab.Today -> TodayRoute()
                Tab.Week -> WeekRoute()
                Tab.Settings -> SettingsRoute(
                    updates = { UpdatesSection(updateState, onCheck = updates::checkNow, onOpenUpdate = updates::openDialog) },
                )
            }
        }
    }
}
