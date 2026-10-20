package io.github.khoryz666.timer.tracking

/** Notices the tracker posts or clears. None of them stop anything from happening. */
interface TrackerNotices {
    /** The gentle "Joy before focus" heads-up. */
    fun nudgeJoyBeforeFocus()

    /** Today's day type was picked, so its prompt can go. */
    fun dismissDayPrompt()
}

fun shouldNudge(enabled: Boolean, starting: TrackTarget, focusBlocksToday: Int): Boolean =
    enabled && starting == TrackTarget.Joy && focusBlocksToday < 1
