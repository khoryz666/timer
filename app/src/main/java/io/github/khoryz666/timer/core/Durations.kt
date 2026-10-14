package io.github.khoryz666.timer.core

import kotlin.math.roundToInt

private const val MINUTE_MS = 60_000L

/** "0m", "45m", "3h", "3h 12m" - rounded down to the minute. */
fun formatDuration(ms: Long): String {
    val totalMinutes = ms.coerceAtLeast(0) / MINUTE_MS
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

/** A running stopwatch: "4:05", "1:02:03". */
fun formatElapsed(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

fun formatPercent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

fun minutesToMs(minutes: Int): Long = minutes * MINUTE_MS
