package dev.x3d.dayline.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Wearable
import dev.x3d.dayline.data.wear.WatchPayloadCodec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Asks the phone app to sync and send today's timetable. The watch never talks to WebUntis. */
object PhoneSync {
    /** @return number of phones that received the request. */
    suspend fun request(context: Context): Int {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        val messages = Wearable.getMessageClient(context)
        var sent = 0
        for (node in nodes) {
            runCatching { messages.sendMessage(node.id, WatchPayloadCodec.SYNC_REQUEST_PATH, ByteArray(0)).await() }
                .onSuccess { sent++ }
        }
        return sent
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
