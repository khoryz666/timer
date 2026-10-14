package io.github.khoryz666.timer.quota

enum class QuotaStatus { BELOW_FLOOR, FLOOR_MET, TARGET_MET }

fun quotaStatus(value: Long, floor: Long, target: Long): QuotaStatus = when {
    value >= target -> QuotaStatus.TARGET_MET
    value >= floor -> QuotaStatus.FLOOR_MET
    else -> QuotaStatus.BELOW_FLOOR
}

/** Engine time is counted in whole blocks: the sum of the day's Engine time divided by the block length. */
data class EngineProgress(
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

    companion object {
        fun of(trackedMs: Long, settings: QuotaSettings): EngineProgress {
            val blockMs = settings.blockMs
            return EngineProgress(
                trackedMs = trackedMs,
                blocks = completedBlocks(trackedMs, blockMs),
                nextBlockFraction = (trackedMs % blockMs).toFloat() / blockMs,
                floorBlocks = settings.floorBlocks,
                targetBlocks = settings.targetBlocks,
                targetMs = settings.engineTargetMs,
            )
        }
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
