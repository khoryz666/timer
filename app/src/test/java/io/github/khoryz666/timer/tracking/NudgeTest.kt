package io.github.khoryz666.timer.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeTest {
    @Test
    fun `nudges only when enabled, starting Joy, with no Engine block yet`() {
        assertTrue(shouldNudge(enabled = true, starting = TrackTarget.Joy, engineBlocksToday = 0))
        assertFalse(shouldNudge(enabled = false, starting = TrackTarget.Joy, engineBlocksToday = 0))
        assertFalse(shouldNudge(enabled = true, starting = TrackTarget.Joy, engineBlocksToday = 1))
        assertFalse(shouldNudge(enabled = true, starting = TrackTarget.Engine(1), engineBlocksToday = 0))
    }
}
