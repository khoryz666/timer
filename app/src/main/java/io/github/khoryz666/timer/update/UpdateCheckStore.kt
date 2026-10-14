package io.github.khoryz666.timer.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/** Remembers when the app last checked for updates on its own. */
class UpdateCheckStore(private val dataStore: DataStore<Preferences>) {

    suspend fun lastAutoCheck(): Long = dataStore.data.first()[LAST_AUTO_CHECK] ?: 0

    suspend fun setLastAutoCheck(epochMs: Long) {
        dataStore.edit { it[LAST_AUTO_CHECK] = epochMs }
    }

    private companion object {
        val LAST_AUTO_CHECK = longPreferencesKey("last_auto_check")
    }
}

const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

/** At most one automatic check per 24 hours (a clock set back also allows one). */
fun isAutoCheckDue(lastCheck: Long, now: Long): Boolean = now - lastCheck !in 0 until AUTO_CHECK_INTERVAL_MS
