package com.example.timetracker.util

object TimeFormatter {
    fun formatHMS(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        return String.format("%02d h %02d m %02d s", hours, minutes, seconds)
    }
}
