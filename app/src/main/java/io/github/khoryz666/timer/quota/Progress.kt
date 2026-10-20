package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.tracking.Pillar

enum class QuotaStatus { BELOW_FLOOR, FLOOR_MET, TARGET_MET }

fun quotaStatus(value: Long, floor: Long, target: Long): QuotaStatus = when {
    value >= target -> QuotaStatus.TARGET_MET
    value >= floor -> QuotaStatus.FLOOR_MET
    else -> QuotaStatus.BELOW_FLOOR
}

/** A focus track's day, counted in whole blocks: the sum of its time divided by the block length. */
data class TrackProgress(
    val pillar: Pillar,
    val trackedMs: Long,
    val blocks: Int,
    /** How far into the next, not-yet-complete block, 0 until 1. */
    val nextBlockFraction: Float,
    val floorBlocks: Int,
    val targetBlocks: Int,
    val targetMs: Long,
) {
    val status: QuotaStatus
        get() = quotaStatus(blocks.toLong(), floorBlocks.toLong(), targetBlocks.toLong())

    val hasTarget: Boolean get() = targetBlocks > 0

    /** Tracked time still needed to reach the target. */
    val remainingMs: Long get() = (targetMs - trackedMs).coerceAtLeast(0)

    companion object {
        fun of(pillar: Pillar, trackedMs: Long, targets: DayTargets, blockMs: Long) = TrackProgress(
            pillar = pillar,
            trackedMs = trackedMs,
            blocks = completedBlocks(trackedMs, blockMs),
            nextBlockFraction = (trackedMs % blockMs).toFloat() / blockMs,
            floorBlocks = targets.floor(pillar),
            targetBlocks = targets.target(pillar),
            targetMs = targets.target(pillar) * blockMs,
        )
    }
}

/** Joy is a single daily target: its floor is the target itself, so it is either below it or met. */
data class JoyProgress(val trackedMs: Long, val targetMs: Long) {
    val status: QuotaStatus get() = quotaStatus(trackedMs, targetMs, targetMs)
    val fraction: Float get() = if (targetMs == 0L) 1f else (trackedMs.toFloat() / targetMs).coerceAtMost(1f)

    companion object {
        fun of(trackedMs: Long, settings: QuotaSettings) = JoyProgress(trackedMs, settings.joyTargetMs)
    }
}

fun completedBlocks(trackedMs: Long, blockMs: Long): Int = (trackedMs.coerceAtLeast(0) / blockMs).toInt()
