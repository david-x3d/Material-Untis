package dev.x3d.dayline.data

import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dev.x3d.dayline.complication.NextLessonComplicationService
import dev.x3d.dayline.data.wear.WatchPayloadCodec
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class TimetableListenerService : WearableListenerService() {
    private val store: WearTimetableStore by inject()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { events ->
            events.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                if (event.dataItem.uri.path != WatchPayloadCodec.PATH) return@forEach
                val bytes = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray(WatchPayloadCodec.KEY_BYTES) ?: return@forEach
                runBlocking { store.save(bytes) }
                ComplicationDataSourceUpdateRequester.create(
                    this,
                    ComponentName(this, NextLessonComplicationService::class.java),
                ).requestUpdateAll()
            }
        }
    }
}
