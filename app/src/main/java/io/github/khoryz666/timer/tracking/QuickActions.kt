package io.github.khoryz666.timer.tracking

sealed interface QuickAction {
    val label: String

    data class Switch(val target: TrackTarget, override val label: String) : QuickAction
    data object Stop : QuickAction {
        override val label = "Stop"
    }
}

/**
 * The notification's buttons (Android shows at most three): Stop while something is running,
 * then the focus track furthest from its target, then Joy, then the other focus tracks. Project
 * starts the week's main project. Whatever is running is left out.
 */
fun quickActions(status: TodayStatus, max: Int = 3): List<QuickAction> {
    val running = status.running?.pillar
    val focus = status.tracks
        .sortedByDescending { it.remainingMs } // stable: ties keep Project, NeetCode, Reading order
        .filter { it.pillar != running }
        .mapNotNull { track ->
            when (track.pillar) {
                Pillar.PROJECT -> status.mainProject?.let { QuickAction.Switch(TrackTarget.Project(it.id), it.name) }
                Pillar.NEETCODE -> QuickAction.Switch(TrackTarget.NeetCode, Pillar.NEETCODE.label)
                Pillar.READING -> QuickAction.Switch(TrackTarget.Reading, Pillar.READING.label)
                Pillar.JOY -> null
            }
        }
    val joy = QuickAction.Switch(TrackTarget.Joy, Pillar.JOY.label).takeIf { running != Pillar.JOY }
    val stop = QuickAction.Stop.takeIf { running != null }
    return (listOfNotNull(stop, focus.firstOrNull(), joy) + focus.drop(1)).take(max)
}
