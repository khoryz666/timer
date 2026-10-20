package io.github.khoryz666.timer.quota

import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.tracking.Pillar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProgressTest {
    private val minute = 60_000L
    private val settings = QuotaSettings() // 60-min blocks, Joy 4h
    private val deep = settings.targets(DayType.DEEP) // Project floor 3 / target 5, NeetCode 1, Reading 2

    private fun track(pillar: Pillar, minutes: Long, targets: DayTargets = deep) =
        TrackProgress.of(pillar, minutes * minute, targets, settings.blockMs)

    @Test
    fun `blocks are now 60 minutes by default`() {
        assertEquals(60, QuotaSettings().blockMinutes)
    }

    @Test
    fun `blocks count whole block lengths of a track's time`() {
        assertEquals(0, track(Pillar.PROJECT, 59).blocks)
        assertEquals(1, track(Pillar.PROJECT, 60).blocks)
        assertEquals(3, track(Pillar.PROJECT, 200).blocks)
    }

    @Test
    fun `next block fraction is the progress into the incomplete block`() {
        assertEquals(0.5f, track(Pillar.READING, 90).nextBlockFraction, 1e-6f)
        assertEquals(0f, track(Pillar.READING, 120).nextBlockFraction, 1e-6f)
    }

    @Test
    fun `project moves from below floor to floor met to target met`() {
        assertEquals(QuotaStatus.BELOW_FLOOR, track(Pillar.PROJECT, 179).status)
        assertEquals(QuotaStatus.FLOOR_MET, track(Pillar.PROJECT, 180).status)
        assertEquals(QuotaStatus.TARGET_MET, track(Pillar.PROJECT, 300).status)
    }

    @Test
    fun `NeetCode and Reading have no floor - below target until it is met`() {
        assertEquals(0, track(Pillar.READING, 0).floorBlocks)
        assertEquals(QuotaStatus.FLOOR_MET, track(Pillar.READING, 60).status)
        assertEquals(QuotaStatus.TARGET_MET, track(Pillar.READING, 120).status)
    }

    @Test
    fun `targets come from the day type`() {
        assertEquals(listOf(3, 3, 5), DayType.entries.map { settings.targets(it).project })
        assertEquals(300 * minute, track(Pillar.PROJECT, 0).targetMs)
        assertEquals(270 * minute, track(Pillar.PROJECT, 30).remainingMs)
        assertEquals(0L, track(Pillar.PROJECT, 400).remainingMs)
    }

    @Test
    fun `a zero target is met at once and counts as no target`() {
        val light = settings.targets(DayType.LIGHT)
        val neet = track(Pillar.NEETCODE, 0, light)
        assertFalse(neet.hasTarget)
        assertEquals(QuotaStatus.TARGET_MET, neet.status)
        assertEquals(0L, neet.remainingMs)
    }

    @Test
    fun `joy is below target until the full target is reached`() {
        assertEquals(QuotaStatus.BELOW_FLOOR, JoyProgress.of(239 * minute, settings).status)
        assertEquals(QuotaStatus.TARGET_MET, JoyProgress.of(240 * minute, settings).status)
        assertEquals(1f, JoyProgress.of(300 * minute, settings).fraction, 1e-6f)
        assertEquals(0.25f, JoyProgress.of(60 * minute, settings).fraction, 1e-6f)
    }

    @Test
    fun `sanitized clamps values and keeps the project floor at or below its target`() {
        val wild = QuotaSettings(
            blockMinutes = 5,
            dayTargets = mapOf(DayType.DEEP to DayTargets(projectFloor = 9, project = 3, neetCode = -1, reading = 40)),
            joyTargetMinutes = -15,
        ).sanitized()

        assertEquals(15, wild.blockMinutes)
        assertEquals(0, wild.joyTargetMinutes)
        assertEquals(DayTargets(projectFloor = 3, project = 3, neetCode = 0, reading = 12), wild.targets(DayType.DEEP))
        // Day types missing from the map get their defaults.
        assertEquals(QuotaSettings.DEFAULT_TARGETS.getValue(DayType.LIGHT), wild.targets(DayType.LIGHT))
    }
}
