package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.core.minutesToMs
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.tracking.Pillar

/** One day type's targets, in blocks. Only Project has a floor. */
data class DayTargets(
    val projectFloor: Int,
    val project: Int,
    val neetCode: Int,
    val reading: Int,
) {
    fun target(pillar: Pillar): Int = when (pillar) {
        Pillar.PROJECT -> project
        Pillar.NEETCODE -> neetCode
        Pillar.READING -> reading
        Pillar.JOY -> error("Joy has no block target")
    }

    fun floor(pillar: Pillar): Int = if (pillar == Pillar.PROJECT) projectFloor else 0

    fun sanitized(): DayTargets {
        val project = project.coerceIn(QuotaSettings.TRACK_BLOCKS)
        return DayTargets(
            projectFloor = projectFloor.coerceIn(0, project),
            project = project,
            neetCode = neetCode.coerceIn(QuotaSettings.TRACK_BLOCKS),
            reading = reading.coerceIn(QuotaSettings.TRACK_BLOCKS),
        )
    }
}

/** Block length, each day type's targets, and the daily Joy target (4h by default). */
data class QuotaSettings(
    val blockMinutes: Int = 60,
    val dayTargets: Map<DayType, DayTargets> = DEFAULT_TARGETS,
    val joyTargetMinutes: Int = 240,
) {
    val blockMs: Long get() = minutesToMs(blockMinutes)
    val joyTargetMs: Long get() = minutesToMs(joyTargetMinutes)

    fun targets(type: DayType): DayTargets = dayTargets[type] ?: DEFAULT_TARGETS.getValue(type)

    fun withTargets(type: DayType, transform: (DayTargets) -> DayTargets) =
        copy(dayTargets = dayTargets + (type to transform(targets(type))))

    /** Clamps every value to a usable range and keeps each floor at or below its target. */
    fun sanitized() = QuotaSettings(
        blockMinutes = blockMinutes.coerceIn(BLOCK_MINUTES),
        dayTargets = DayType.entries.associateWith { targets(it).sanitized() },
        joyTargetMinutes = joyTargetMinutes.coerceIn(JOY_TARGET_MINUTES),
    )

    companion object {
        val BLOCK_MINUTES = 15..240
        val TRACK_BLOCKS = 0..12
        val JOY_TARGET_MINUTES = 0..720

        /** From the semester routine: Tue-Thu are Deep, Mon/Fri Class, weekends Light. */
        val DEFAULT_TARGETS = mapOf(
            DayType.LIGHT to DayTargets(projectFloor = 1, project = 3, neetCode = 0, reading = 0),
            DayType.CLASS to DayTargets(projectFloor = 2, project = 3, neetCode = 1, reading = 1),
            DayType.DEEP to DayTargets(projectFloor = 3, project = 5, neetCode = 1, reading = 2),
        )
    }
}
