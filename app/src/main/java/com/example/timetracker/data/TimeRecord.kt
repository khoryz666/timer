package com.example.timetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class TimeRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val workDurationMs: Long = 0L,
    val selfDurationMs: Long = 0L,
    val sleepDurationMs: Long = 0L
)
