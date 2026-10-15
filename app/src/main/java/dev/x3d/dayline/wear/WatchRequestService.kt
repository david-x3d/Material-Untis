package dev.x3d.dayline.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dev.x3d.dayline.data.wear.WatchPayloadCodec
import dev.x3d.dayline.sync.SyncScheduler
import org.koin.android.ext.android.inject

/** Receives "please sync" from the watch and runs a sync, which ends with a push to the watch. */
class WatchRequestService : WearableListenerService() {
    private val scheduler: SyncScheduler by inject()

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WatchPayloadCodec.SYNC_REQUEST_PATH) {
            scheduler.runNow()
        }
    }
}
