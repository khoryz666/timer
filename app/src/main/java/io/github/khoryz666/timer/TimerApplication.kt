package io.github.khoryz666.timer

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.core.AppDatabase
import io.github.khoryz666.timer.settings.SettingsStore
import io.github.khoryz666.timer.tracking.Tracker
import io.github.khoryz666.timer.tracking.TrackingNotifications

class TimerApplication : Application() {
    val graph by lazy { AppGraph(this) }
}

val Context.appGraph: AppGraph get() = (applicationContext as TimerApplication).graph

private val Context.settingsDataStore by preferencesDataStore("settings")

/** The app's long-lived objects, created on first use. */
class AppGraph(context: Context) {
    private val app = context.applicationContext

    val clock: AppClock = AppClock.System
    val database by lazy { AppDatabase.build(app) }
    val settings by lazy { SettingsStore(app.settingsDataStore) }
    val notifications by lazy { TrackingNotifications(app) }
    val tracker by lazy { Tracker(database.sessions(), database.projects(), settings, clock, notifications) }
}
