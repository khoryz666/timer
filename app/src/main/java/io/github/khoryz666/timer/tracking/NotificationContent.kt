package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.core.formatDuration
import io.github.khoryz666.timer.tracking.Pillar.ENGINE
import io.github.khoryz666.timer.tracking.Pillar.JOY

/** What the persistent notification shows; the elapsed time itself is a system chronometer. */
data class NotificationContent(
    val title: String,
    val text: String?,
    val runningSince: Long?,
    val actions: List<QuickAction>,
) {
    companion object {
        val Loading = NotificationContent("Timer", null, null, emptyList())

        fun of(status: TodayStatus): NotificationContent {
            val running = status.running
            val title = when (running?.pillar) {
                ENGINE -> "Engine · ${status.runningProjectName ?: "?"}"
                JOY -> "Joy"
                null -> "Not tracking"
            }
            val engine = status.engine
            val joy = status.joy
            val text = "Engine ${engine.blocks}/${engine.targetBlocks} blocks · " +
                "Joy ${formatDuration(joy.trackedMs)}/${formatDuration(joy.targetMs)}"
            return NotificationContent(title, text, running?.startedAt, quickActions(running?.target, status.projects))
        }
    }
}
