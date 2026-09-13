package com.example.timetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(records: List<TimeRecord>) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Time History") })
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
            Text("Work: ${TimeFormatter.formatHMS(record.workDurationMs)}")
            Text("Self: ${TimeFormatter.formatHMS(record.selfDurationMs)}")
            Text("Sleep: ${TimeFormatter.formatHMS(record.sleepDurationMs)}")
        }
    }
}
