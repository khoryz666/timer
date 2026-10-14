package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.projects.Project

sealed interface QuickAction {
    val label: String

    data class Switch(val target: TrackTarget, override val label: String) : QuickAction
    data object Stop : QuickAction {
        override val label = "Stop"
    }
}

/**
 * The notification's buttons (Android shows at most three): Stop while something is running,
 * then the most useful switches. From Engine the obvious switch is Joy; otherwise projects
 * come first, in order.
 */
fun quickActions(running: TrackTarget?, projects: List<Project>, max: Int = 3): List<QuickAction> {
    val engine = projects.map { QuickAction.Switch(TrackTarget.Engine(it.id), it.name) }
    val joy = QuickAction.Switch(TrackTarget.Joy, "Joy")
    val switches = (if (running is TrackTarget.Engine) listOf(joy) + engine else engine + joy)
        .filter { it.target != running }
    val stop = if (running != null) listOf(QuickAction.Stop) else emptyList()
    return (stop + switches).take(max)
}
