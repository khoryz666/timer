package io.github.khoryz666.timer.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeTest {
    @Test
    fun `nudges only when enabled, starting Joy, with no focus block yet`() {
        assertTrue(shouldNudge(enabled = true, starting = TrackTarget.Joy, focusBlocksToday = 0))
        assertFalse(shouldNudge(enabled = false, starting = TrackTarget.Joy, focusBlocksToday = 0))
        assertFalse(shouldNudge(enabled = true, starting = TrackTarget.Joy, focusBlocksToday = 1))
        assertFalse(shouldNudge(enabled = true, starting = TrackTarget.Project(1), focusBlocksToday = 0))
        assertFalse(shouldNudge(enabled = true, starting = TrackTarget.Reading, focusBlocksToday = 0))
    }
}
