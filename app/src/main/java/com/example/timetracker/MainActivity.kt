package com.example.timetracker

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timetracker.service.TimeTrackerService
import com.example.timetracker.ui.DashboardScreen
import com.example.timetracker.ui.HistoryScreen
import com.example.timetracker.ui.HistoryViewModel
import com.example.timetracker.ui.TrackerViewModel
import com.example.timetracker.ui.theme.TimeTrackerTheme

private enum class Destination(val route: String, val label: String) {
    Dashboard("dashboard", "Timer"),
    History("history", "History")
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            manageForegroundService(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Service Unconditionally
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                manageForegroundService(true)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            manageForegroundService(true)
        }

        enableEdgeToEdge()
        setContent {
            TimeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimeTrackerApp(applicationContext as Application)
                }
            }
        }
    }

    private fun manageForegroundService(start: Boolean) {
        val intent = Intent(this, TimeTrackerService::class.java)
        if (start) {
            intent.action = TimeTrackerService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            intent.action = TimeTrackerService.ACTION_STOP_SERVICE
            startService(intent) // Send stop signal
        }
    }
}

@Composable
fun TimeTrackerApp(application: Application) {
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
                                imageVector = if (destination == Destination.Dashboard) Icons.Filled.Home else Icons.Filled.List,
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
