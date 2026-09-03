package dev.x3d.dayline.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.x3d.dayline.data.repo.PeriodRepositoryImpl
import dev.x3d.dayline.data.wear.WatchPayloadCodec
import dev.x3d.dayline.domain.model.WatchPayload
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class WearableWatchPusher(private val context: Context) : PeriodRepositoryImpl.WatchPusher {
    override suspend fun push(payload: WatchPayload) {
        val bytes = WatchPayloadCodec.encode(payload)
        val request = PutDataMapRequest.create(WatchPayloadCodec.PATH).apply {
            dataMap.putByteArray(WatchPayloadCodec.KEY_BYTES, bytes)
            dataMap.putLong("syncedAt", payload.syncedAt)
        }.asPutDataRequest().setUrgent()
        val task = Wearable.getDataClient(context).putDataItem(request)
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(Unit) }
            task.addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
