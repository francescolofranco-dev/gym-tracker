package dev.francescolofranco.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val WEEK_MODE = stringPreferencesKey("week_mode")
        val DRIVE_BACKUP_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val HAS_OFFERED_DRIVE_RESTORE = booleanPreferencesKey("has_offered_drive_restore")
    }

    val unit: Flow<WeightUnit> = context.dataStore.data.map { p ->
        runCatching { WeightUnit.valueOf(p[Keys.UNIT] ?: WeightUnit.KG.name) }.getOrDefault(WeightUnit.KG)
    }

    val weekMode: Flow<WeekMode> = context.dataStore.data.map { p ->
        runCatching { WeekMode.valueOf(p[Keys.WEEK_MODE] ?: WeekMode.ROLLING_7.name) }.getOrDefault(WeekMode.ROLLING_7)
    }

    val driveBackupEnabled: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.DRIVE_BACKUP_ENABLED] ?: false
    }

    val lastBackupAt: Flow<Instant?> = context.dataStore.data.map { p ->
        p[Keys.LAST_BACKUP_AT]?.let(Instant::ofEpochMilli)
    }

    val hasOfferedDriveRestore: Flow<Boolean> = context.dataStore.data.map { p ->
        p[Keys.HAS_OFFERED_DRIVE_RESTORE] ?: false
    }

    suspend fun setUnit(unit: WeightUnit) {
        context.dataStore.edit { it[Keys.UNIT] = unit.name }
    }

    suspend fun setWeekMode(mode: WeekMode) {
        context.dataStore.edit { it[Keys.WEEK_MODE] = mode.name }
    }

    suspend fun setDriveBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DRIVE_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastBackupAt(at: Instant) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = at.toEpochMilli() }
    }

    suspend fun setHasOfferedDriveRestore(offered: Boolean) {
        context.dataStore.edit { it[Keys.HAS_OFFERED_DRIVE_RESTORE] = offered }
    }
}
