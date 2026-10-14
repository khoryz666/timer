package io.github.khoryz666.timer.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import io.github.khoryz666.timer.quota.QuotaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val quota: Flow<QuotaSettings> = dataStore.data.map { it.toQuota() }.distinctUntilChanged()

    /** Gentle notice when Joy starts before the first Engine block of the day. Off by default. */
    val nudgeEnabled: Flow<Boolean> = dataStore.data.map { it[NUDGE] ?: false }.distinctUntilChanged()

    suspend fun updateQuota(transform: (QuotaSettings) -> QuotaSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toQuota()).sanitized()
            prefs[BLOCK_MINUTES] = updated.blockMinutes
            prefs[FLOOR_BLOCKS] = updated.floorBlocks
            prefs[TARGET_BLOCKS] = updated.targetBlocks
            prefs[WEEKLY_TARGET_BLOCKS] = updated.weeklyTargetBlocks
            prefs[JOY_TARGET_MINUTES] = updated.joyTargetMinutes
        }
    }

    suspend fun setNudgeEnabled(enabled: Boolean) {
        dataStore.edit { it[NUDGE] = enabled }
    }

    suspend fun isNudgeEnabled(): Boolean = nudgeEnabled.first()

    private fun Preferences.toQuota(): QuotaSettings {
        val defaults = QuotaSettings()
        return QuotaSettings(
            blockMinutes = this[BLOCK_MINUTES] ?: defaults.blockMinutes,
            floorBlocks = this[FLOOR_BLOCKS] ?: defaults.floorBlocks,
            targetBlocks = this[TARGET_BLOCKS] ?: defaults.targetBlocks,
            weeklyTargetBlocks = this[WEEKLY_TARGET_BLOCKS] ?: defaults.weeklyTargetBlocks,
            joyTargetMinutes = this[JOY_TARGET_MINUTES] ?: defaults.joyTargetMinutes,
        )
    }

    private companion object {
        val BLOCK_MINUTES = intPreferencesKey("block_minutes")
        val FLOOR_BLOCKS = intPreferencesKey("floor_blocks")
        val TARGET_BLOCKS = intPreferencesKey("target_blocks")
        val WEEKLY_TARGET_BLOCKS = intPreferencesKey("weekly_target_blocks")
        val JOY_TARGET_MINUTES = intPreferencesKey("joy_target_minutes")
        val NUDGE = booleanPreferencesKey("nudge_enabled")
    }
}
