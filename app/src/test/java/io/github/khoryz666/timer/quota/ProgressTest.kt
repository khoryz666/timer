package io.github.khoryz666.timer.quota

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {
    private val minute = 60_000L
    private val settings = QuotaSettings() // 90-min blocks, floor 2, target 4, Joy 4h

    @Test
    fun `blocks count whole block lengths of summed Engine time`() {
        assertEquals(0, EngineProgress.of(89 * minute, settings).blocks)
        assertEquals(1, EngineProgress.of(90 * minute, settings).blocks)
        assertEquals(2, EngineProgress.of(200 * minute, settings).blocks)
        assertEquals(5, EngineProgress.of(450 * minute, settings).blocks)
    }

    @Test
    fun `next block fraction is the progress into the incomplete block`() {
        assertEquals(0.5f, EngineProgress.of(135 * minute, settings).nextBlockFraction, 1e-6f)
        assertEquals(0f, EngineProgress.of(180 * minute, settings).nextBlockFraction, 1e-6f)
    }

    @Test
    fun `engine status moves from below floor to floor met to target met`() {
        assertEquals(QuotaStatus.BELOW_FLOOR, EngineProgress.of(179 * minute, settings).status)
        assertEquals(QuotaStatus.FLOOR_MET, EngineProgress.of(180 * minute, settings).status)
        assertEquals(QuotaStatus.FLOOR_MET, EngineProgress.of(359 * minute, settings).status)
        assertEquals(QuotaStatus.TARGET_MET, EngineProgress.of(360 * minute, settings).status)
    }

    @Test
    fun `engine target time is target blocks times block length`() {
        assertEquals(360 * minute, EngineProgress.of(0, settings).targetMs)
        assertEquals(240 * minute, EngineProgress.of(0, settings.copy(blockMinutes = 60)).targetMs)
    }

    @Test
    fun `a zero floor is met immediately`() {
        assertEquals(QuotaStatus.FLOOR_MET, EngineProgress.of(0, settings.copy(floorBlocks = 0)).status)
    }

    @Test
    fun `joy is below target until the full target is reached`() {
        assertEquals(QuotaStatus.BELOW_FLOOR, JoyProgress.of(239 * minute, settings).status)
        assertEquals(QuotaStatus.TARGET_MET, JoyProgress.of(240 * minute, settings).status)
        assertEquals(1f, JoyProgress.of(300 * minute, settings).fraction, 1e-6f)
        assertEquals(0.25f, JoyProgress.of(60 * minute, settings).fraction, 1e-6f)
    }

    @Test
    fun `sanitized clamps values and keeps the floor at or below the target`() {
        val sanitized = QuotaSettings(blockMinutes = 5, floorBlocks = 9, targetBlocks = 3, weeklyTargetBlocks = 0, joyTargetMinutes = -15).sanitized()
        assertEquals(QuotaSettings(blockMinutes = 15, floorBlocks = 3, targetBlocks = 3, weeklyTargetBlocks = 1, joyTargetMinutes = 0), sanitized)
    }
}
