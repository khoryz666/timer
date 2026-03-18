package com.example.timetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timetracker.data.LocalDatabase
import com.example.timetracker.data.TrackerPreferences
import com.example.timetracker.service.TimeTrackerService
import com.example.timetracker.ui.DashboardScreen
import com.example.timetracker.ui.HistoryScreen
import com.example.timetracker.ui.TrackerViewModel
import com.example.timetracker.ui.theme.TimeTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: TrackerPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            manageForegroundService(true)
        } else {
            // Permission denied, handle gracefully (e.g., reset toggle in UI)
            CoroutineScope(Dispatchers.IO).launch {
                prefs.setEnableService(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TrackerPreferences(applicationContext)

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
                    TimeTrackerApp(prefs, applicationContext as android.app.Application)
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
fun TimeTrackerApp(prefs: TrackerPreferences, application: android.app.Application) {
    val navController = rememberNavController()
    val viewModel: TrackerViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
    val coroutineScope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            val uiState by viewModel.uiState.collectAsState()
            val isNotificationEnabled by prefs.showNotificationFlow.collectAsState(initial = false)
            DashboardScreen(
                uiState = uiState,
                isNotificationEnabled = isNotificationEnabled,
                onNavigateToHistory = { navController.navigate("history") },
                onStateChange = { newState -> viewModel.onButtonPress(newState) },
                onResetCurrent = { viewModel.resetActiveTimer() },
                onForceSave = { viewModel.forceSaveCurrentProgress() },
                onToggleNotification = { enabled -> 
                    coroutineScope.launch { prefs.setShowNotification(enabled) } 
                }
            )
        }
        composable("history") {
            HistoryScreen(
                database = LocalDatabase.getDatabase(application),
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
