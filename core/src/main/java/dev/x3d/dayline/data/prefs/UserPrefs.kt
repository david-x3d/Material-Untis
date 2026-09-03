package dev.x3d.dayline.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPrefs(context: Context) {
    private val store = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("dayline_prefs")
    }

    val syncIntervalMinutes: Flow<Int> = store.data.map { it[SYNC_MINUTES] ?: DEFAULT_SYNC_MINUTES }
    val lastImportTime: Flow<Long?> = store.data.map { it[LAST_IMPORT] }
    val lastSyncAt: Flow<Long?> = store.data.map { it[LAST_SYNC] }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        store.edit { it[SYNC_MINUTES] = minutes.coerceAtLeast(15) }
    }

    suspend fun setLastImportTime(value: Long) {
        store.edit { it[LAST_IMPORT] = value }
    }

    suspend fun setLastSyncAt(value: Long) {
        store.edit { it[LAST_SYNC] = value }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_SYNC_MINUTES = 15
        private val SYNC_MINUTES = intPreferencesKey("sync_minutes")
        private val LAST_IMPORT = longPreferencesKey("last_import")
        private val LAST_SYNC = longPreferencesKey("last_sync")
    }
}
