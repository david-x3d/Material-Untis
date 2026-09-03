package dev.x3d.dayline.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.x3d.dayline.data.wear.WatchPayloadCodec
import dev.x3d.dayline.domain.model.WatchPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WearTimetableStore(context: Context) {
    private val store = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("dayline_watch")
    }

    val payload: Flow<WatchPayload?> = store.data.map { prefs ->
        prefs[BYTES]?.let { runCatching { WatchPayloadCodec.decode(it) }.getOrNull() }
    }

    suspend fun save(bytes: ByteArray) {
        store.edit { it[BYTES] = bytes }
    }

    companion object {
        private val BYTES = byteArrayPreferencesKey("payload")
    }
}
