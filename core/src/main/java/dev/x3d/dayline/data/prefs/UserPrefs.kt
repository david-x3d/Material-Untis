package dev.x3d.dayline.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.x3d.dayline.domain.model.SubjectColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPrefs(context: Context) {
    private val store = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("dayline_prefs")
    }

    val syncIntervalMinutes: Flow<Int> = store.data.map { it[SYNC_MINUTES] ?: DEFAULT_SYNC_MINUTES }
    val lastImportTime: Flow<Long?> = store.data.map { it[LAST_IMPORT] }
    val lastSyncAt: Flow<Long?> = store.data.map { it[LAST_SYNC] }

    /** Wallpaper colours (Android 12+) instead of the Material-Untis palette. Off by default. */
    val dynamicColor: Flow<Boolean> = store.data.map { it[DYNAMIC_COLOR] ?: false }

    val subjectColors: Flow<SubjectColors> = store.data.map { prefs ->
        SubjectColors(
            custom = prefs[COLORS_CUSTOM] ?: false,
            seed = prefs[COLORS_SEED] ?: 0,
            picked = decodeColors(prefs[COLORS_PICKED]),
        )
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSubjectColors(colors: SubjectColors) {
        store.edit {
            it[COLORS_CUSTOM] = colors.custom
            it[COLORS_SEED] = colors.seed
            it[COLORS_PICKED] = encodeColors(colors.picked)
        }
    }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        store.edit { it[SYNC_MINUTES] = minutes.coerceAtLeast(15) }
    }

    suspend fun setLastImportTime(value: Long) {
        store.edit { it[LAST_IMPORT] = value }
    }

    suspend fun setLastSyncAt(value: Long) {
        store.edit { it[LAST_SYNC] = value }
    }

    /** Logout keeps the look (colours) and drops everything account related. */
    suspend fun clear() {
        store.edit {
            it.remove(SYNC_MINUTES)
            it.remove(LAST_IMPORT)
            it.remove(LAST_SYNC)
        }
    }

    companion object {
        const val DEFAULT_SYNC_MINUTES = 15
        private val SYNC_MINUTES = intPreferencesKey("sync_minutes")
        private val LAST_IMPORT = longPreferencesKey("last_import")
        private val LAST_SYNC = longPreferencesKey("last_sync")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val COLORS_CUSTOM = booleanPreferencesKey("subject_colors_custom")
        private val COLORS_SEED = intPreferencesKey("subject_colors_seed")
        private val COLORS_PICKED = stringPreferencesKey("subject_colors_picked")

        // "M=-1234;D=-5678": subject short names never contain ';' or '='.
        fun encodeColors(map: Map<String, Int>): String =
            map.entries.joinToString(";") { (subject, argb) -> "${subject.replace(";", "").replace("=", "")}=$argb" }

        fun decodeColors(raw: String?): Map<String, Int> =
            raw.orEmpty().split(';').mapNotNull { entry ->
                val key = entry.substringBefore('=', "")
                val value = entry.substringAfter('=', "").toIntOrNull()
                if (key.isEmpty() || value == null) null else key to value
            }.toMap()
    }
}
