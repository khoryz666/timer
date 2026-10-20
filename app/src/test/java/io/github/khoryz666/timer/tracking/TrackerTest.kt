package io.github.khoryz666.timer.tracking

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.khoryz666.timer.core.AppDatabase
import io.github.khoryz666.timer.core.FixedClock
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Runs the tracker against a real (in-memory) database. Every step awaits a suspend call or
 * takes the first value of a flow - nothing waits on wall-clock time - so it's deterministic.
 */
@RunWith(RobolectricTestRunner::class)
class TrackerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val minute = 60_000L
    private val clock = FixedClock(LocalDateTime.of(2026, 10, 14, 9, 0))
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .addCallback(AppDatabase.SeedProjects)
        .build()
    private var nudges = 0
    private var promptDismissals = 0
    private val notices = object : TrackerNotices {
        override fun nudgeJoyBeforeFocus() {
            nudges++
        }

        override fun dismissDayPrompt() {
            promptDismissals++
        }
    }

    @After
    fun closeDb() = db.close()

    private fun TestScope.newSettings() = SettingsStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("settings.preferences_pb") }
    )

    private fun tracker(settings: SettingsStore) =
        Tracker(db.sessions(), db.projects(), db.dayTypes(), settings, clock, notices)

    private suspend fun today(tracker: Tracker) = tracker.today(tickMs = 1_000).first()

    @Test
    fun `switching ends the running session and starts the next at the same instant`() = runTest {
        val tracker = tracker(newSettings())
        tracker.switchTo(TrackTarget.Project(1))
        clock.nowMs += 30 * minute
        tracker.switchTo(TrackTarget.NeetCode)

        val sessions = db.sessions().endingIn(0, Long.MAX_VALUE)
        assertEquals(2, sessions.size)
        assertEquals(sessions[0].endedAt, sessions[1].startedAt)
        assertEquals(TrackTarget.NeetCode, db.sessions().running()?.target)
    }

    @Test
    fun `switching to what is already running changes nothing`() = runTest {
        val tracker = tracker(newSettings())
        tracker.switchTo(TrackTarget.Reading)
        val first = db.sessions().running()
        clock.nowMs += 10 * minute

        tracker.switchTo(TrackTarget.Reading)

        assertEquals(first, db.sessions().running())
        assertEquals(1, db.sessions().endingIn(0, Long.MAX_VALUE).size)
    }

    @Test
    fun `stop ends the running session`() = runTest {
        val tracker = tracker(newSettings())
        tracker.switchTo(TrackTarget.Joy)
        clock.nowMs += 45 * minute

        tracker.stop()

        assertNull(db.sessions().running())
        assertEquals(45 * minute, today(tracker).joy.trackedMs)
    }

    @Test
    fun `a running session survives a restart - a fresh tracker picks it up from the database`() = runTest {
        val settings = newSettings()
        tracker(settings).switchTo(TrackTarget.Project(1))
        clock.nowMs += 100 * minute // process killed, phone rebooted...

        val status = today(tracker(settings))

        assertEquals(TrackTarget.Project(1), status.running?.target)
        assertEquals("QorFinder", status.runningProjectName)
        assertEquals(1, status.track(Pillar.PROJECT).blocks) // 60-min blocks
    }

    @Test
    fun `each track is counted separately`() = runTest {
        val tracker = tracker(newSettings())
        tracker.switchTo(TrackTarget.NeetCode)
        clock.nowMs += 60 * minute
        tracker.switchTo(TrackTarget.Project(2))
        clock.nowMs += 150 * minute
        tracker.switchTo(TrackTarget.Reading)
        clock.nowMs += 30 * minute

        val status = today(tracker)

        assertEquals(listOf(2, 1, 0), listOf(Pillar.PROJECT, Pillar.NEETCODE, Pillar.READING).map { status.track(it).blocks })
        assertEquals(mapOf(2L to 150 * minute), status.totals.byProject)
    }

    @Test
    fun `a session running across midnight counts toward the new day`() = runTest {
        clock.set(LocalDateTime.of(2026, 10, 14, 23, 0))
        val tracker = tracker(newSettings())
        tracker.switchTo(TrackTarget.Project(1))
        clock.set(LocalDateTime.of(2026, 10, 15, 1, 0))

        val status = today(tracker)

        assertEquals(clock.today(), status.date)
        assertEquals(120 * minute, status.track(Pillar.PROJECT).trackedMs)
    }

    @Test
    fun `the day type is the default until chosen, then carries over to later days`() = runTest {
        val tracker = tracker(newSettings())
        val before = today(tracker)
        assertEquals(DayType.DEFAULT, before.dayType)
        assertFalse(before.dayTypeChosen)

        tracker.setDayType(DayType.DEEP)
        val chosen = today(tracker)
        assertEquals(DayType.DEEP, chosen.dayType)
        assertTrue(chosen.dayTypeChosen)
        assertEquals(5, chosen.track(Pillar.PROJECT).targetBlocks)
        assertEquals(1, promptDismissals)

        clock.set(LocalDateTime.of(2026, 10, 15, 9, 0))
        val nextDay = today(tracker)
        assertEquals(DayType.DEEP, nextDay.dayType)
        assertFalse(nextDay.dayTypeChosen)
    }

    @Test
    fun `nudges when Joy starts before the first focus block, only if enabled`() = runTest {
        val settings = newSettings()
        val tracker = tracker(settings)

        tracker.switchTo(TrackTarget.Joy)
        assertEquals(0, nudges) // off by default

        tracker.stop()
        settings.setNudgeEnabled(true)
        tracker.switchTo(TrackTarget.Joy)
        assertEquals(1, nudges)

        tracker.switchTo(TrackTarget.Reading)
        clock.nowMs += 60 * minute
        tracker.switchTo(TrackTarget.Joy)
        assertEquals(1, nudges) // a Reading block is done now
    }

    @Test
    fun `today lists this week's projects`() = runTest {
        val status = today(tracker(newSettings()))
        assertEquals(listOf("QorFinder", "KEFOS"), status.projects.map { it.name })
        assertEquals(listOf(80, 20), status.projects.map { it.sharePercent })
    }
}
