package com.example.timetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.LocalDatabase
import com.example.timetracker.data.TimeRecord
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    database: LocalDatabase,
    onNavigateBack: () -> Unit
) {
    val recordsFlow = remember { database.timeRecordDao().getAllRecordsDescending() }
    val records by recordsFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (records.isEmpty()) {
                item {
                    Text(
                        "No history available.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(records) { record ->
                    HistoryCard(record)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(record: TimeRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = record.date, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Work: ${com.example.timetracker.util.TimeFormatter.formatHMS(record.workDurationMs)}")
            Text("Self: ${com.example.timetracker.util.TimeFormatter.formatHMS(record.selfDurationMs)}")
            Text("Sleep: ${com.example.timetracker.util.TimeFormatter.formatHMS(record.sleepDurationMs)}")
        }
    }
}
