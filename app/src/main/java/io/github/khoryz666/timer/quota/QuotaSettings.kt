package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.minutesToMs

/** Daily and weekly targets. Defaults: 4 x 90-min Engine blocks (floor 2), 24 a week, 4h Joy. */
data class QuotaSettings(
    val blockMinutes: Int = 90,
    val floorBlocks: Int = 2,
    val targetBlocks: Int = 4,
    val weeklyTargetBlocks: Int = 24,
    val joyTargetMinutes: Int = 240,
) {
    val blockMs: Long get() = minutesToMs(blockMinutes)
    val engineTargetMs: Long get() = targetBlocks * blockMs
    val joyTargetMs: Long get() = minutesToMs(joyTargetMinutes)

    /** Clamps every value to a usable range and keeps the floor at or below the target. */
    fun sanitized(): QuotaSettings {
        val target = targetBlocks.coerceIn(TARGET_BLOCKS)
        return QuotaSettings(
            blockMinutes = blockMinutes.coerceIn(BLOCK_MINUTES),
            floorBlocks = floorBlocks.coerceIn(0, target),
            targetBlocks = target,
            weeklyTargetBlocks = weeklyTargetBlocks.coerceIn(WEEKLY_TARGET_BLOCKS),
            joyTargetMinutes = joyTargetMinutes.coerceIn(JOY_TARGET_MINUTES),
        )
    }

    companion object {
        val BLOCK_MINUTES = 15..240
        val TARGET_BLOCKS = 1..12
        val WEEKLY_TARGET_BLOCKS = 1..84
        val JOY_TARGET_MINUTES = 0..720
    }
}
