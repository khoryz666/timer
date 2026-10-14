package io.github.khoryz666.timer.tracking

/** Posts the gentle "Joy before Engine" notice. It never stops anything from starting. */
fun interface Nudger {
    fun nudgeJoyBeforeEngine()
}

fun shouldNudge(enabled: Boolean, starting: TrackTarget, engineBlocksToday: Int): Boolean =
    enabled && starting == TrackTarget.Joy && engineBlocksToday < 1
