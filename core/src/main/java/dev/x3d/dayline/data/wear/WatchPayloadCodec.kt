package dev.x3d.dayline.data.wear

import dev.x3d.dayline.domain.model.WatchPayload
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.json.Json

object WatchPayloadCodec {
    const val PATH = "/dayline/today"
    const val KEY_BYTES = "payload"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(payload: WatchPayload): ByteArray {
        val raw = json.encodeToString(WatchPayload.serializer(), payload).encodeToByteArray()
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw) }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): WatchPayload {
        val raw = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        return json.decodeFromString(WatchPayload.serializer(), raw.decodeToString())
    }
}
