package com.example.timer.ui

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class Destination(val route: String, val label: String) {
    Dashboard("dashboard", "Timer"),
    History("history", "History")
}

/** App-level navigation shell: bottom nav bar + the Dashboard/History destinations. */
@Composable
fun TimerApp(application: Application) {
    val navController = rememberNavController()
    val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val trackerViewModel: TrackerViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        bottomBar = {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (destination == Destination.Dashboard) Icons.Filled.Home else Icons.AutoMirrored.Filled.List,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destination.Dashboard.route) {
                val uiState by trackerViewModel.uiState.collectAsState()
                DashboardScreen(
                    uiState = uiState,
                    onStateChange = { newState -> trackerViewModel.onButtonPress(newState) },
                    onResetCurrent = { trackerViewModel.resetActiveTimer() },
                    onForceSave = { trackerViewModel.forceSaveCurrentProgress() }
                )
            }
            composable(Destination.History.route) {
                val records by historyViewModel.records.collectAsState()
                HistoryScreen(records = records)
            }
        }
    }
}
