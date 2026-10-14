package io.github.khoryz666.timer

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.core.AppDatabase
import io.github.khoryz666.timer.settings.SettingsStore
import io.github.khoryz666.timer.tracking.Tracker
import io.github.khoryz666.timer.tracking.TrackingNotifications
import io.github.khoryz666.timer.update.ApkInstaller
import io.github.khoryz666.timer.update.GitHubReleases
import io.github.khoryz666.timer.update.UpdateCheckStore

class TimerApplication : Application() {
    val graph by lazy { AppGraph(this) }
}

val Context.appGraph: AppGraph get() = (applicationContext as TimerApplication).graph

private val Context.settingsDataStore by preferencesDataStore("settings")
private val Context.updatesDataStore by preferencesDataStore("updates")

/** The app's long-lived objects, created on first use. */
class AppGraph(context: Context) {
    private val app = context.applicationContext

    val clock: AppClock = AppClock.System
    val database by lazy { AppDatabase.build(app) }
    val settings by lazy { SettingsStore(app.settingsDataStore) }
    val notifications by lazy { TrackingNotifications(app) }
    val tracker by lazy { Tracker(database.sessions(), database.projects(), settings, clock, notifications) }

    val releases by lazy { GitHubReleases(UPDATE_REPO, "Timer/${BuildConfig.VERSION_NAME}") }
    val installer by lazy { ApkInstaller(app) }
    val updateChecks by lazy { UpdateCheckStore(app.updatesDataStore) }

    private companion object {
        const val UPDATE_REPO = "khoryz666/timer"
    }
}
