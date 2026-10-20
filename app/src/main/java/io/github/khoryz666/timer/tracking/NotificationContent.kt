package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.quota.QuotaStatus
import io.github.khoryz666.timer.quota.TrackProgress

/**
 * What the persistent notification shows. Collapsed: [title] and the one-line [text]; expanded:
 * one of [lines] per track, then a button for each of [actions]. The day type goes in the
 * header ([dayType]), so the collapsed line fits a phone's width. The elapsed time itself is a
 * system chronometer.
 */
data class NotificationContent(
    val title: String,
    val dayType: String?,
    val text: String?,
    val lines: List<String>,
    val runningSince: Long?,
    val actions: List<QuickAction>,
) {
    companion object {
        val Loading = NotificationContent("Timer", null, null, emptyList(), null, emptyList())

        fun of(status: TodayStatus): NotificationContent {
            val running = status.running
            val title = when (running?.pillar) {
                Pillar.PROJECT -> "Project · ${status.runningProjectName ?: "?"}"
                null -> "Not tracking"
                else -> running.pillar.label
            }
            // A track with nothing to do and nothing done today would only be noise.
            val shown = status.tracks.filter { it.hasTarget || it.trackedMs > 0 || it.pillar == running?.pillar }
            val day = "${status.dayType.label} day" + if (status.dayTypeChosen) "" else "?"
            val text = shown.joinToString(" · ") { "${it.pillar.shortLabel} ${fraction(it)}" }
            val lines = shown.map(::line) + "Joy ${formatDuration(status.joy.trackedMs)} / ${formatDuration(status.joy.targetMs)}"
            return NotificationContent(title, day, text, lines, running?.startedAt, quickActions(status))
        }

        private fun fraction(track: TrackProgress) =
            if (track.hasTarget) "${track.blocks}/${track.targetBlocks}" else "${track.blocks}"

        private fun line(track: TrackProgress): String {
            val unit = if (track.pillar == Pillar.PROJECT) " blocks" else ""
            val note = when {
                !track.hasTarget -> " · no target"
                track.status == QuotaStatus.TARGET_MET -> " ✓"
                track.status == QuotaStatus.BELOW_FLOOR -> " (floor ${track.floorBlocks})"
                else -> ""
            }
            return "${track.pillar.label} ${fraction(track)}$unit$note"
        }
    }
}
