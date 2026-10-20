package io.github.khoryz666.timer.tracking

sealed interface QuickAction {
    val label: String

    data class Switch(val target: TrackTarget, override val label: String) : QuickAction
    data object Stop : QuickAction {
        override val label = "Stop"
    }
}

/**
 * The notification's buttons: Stop while something is running, then this week's projects,
 * NeetCode, Reading and Joy - always in that order, so each button stays in the same place.
 * Whatever is running is left out.
 */
fun quickActions(status: TodayStatus): List<QuickAction> {
    val running = status.running?.target
    val switches = status.projects.map { QuickAction.Switch(TrackTarget.Project(it.id), it.name) } +
        listOf(TrackTarget.NeetCode, TrackTarget.Reading, TrackTarget.Joy).map { QuickAction.Switch(it, it.pillar.label) }
    return listOfNotNull(QuickAction.Stop.takeIf { running != null }) + switches.filter { it.target != running }
}
