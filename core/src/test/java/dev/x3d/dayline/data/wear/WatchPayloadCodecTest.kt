package dev.x3d.dayline.data.wear

import dev.x3d.dayline.domain.model.WatchPayload
import dev.x3d.dayline.domain.model.WatchPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPayloadCodecTest {
    @Test
    fun roundTripStableAndSmall() {
        val payload = WatchPayload(
            date = 20260903,
            syncedAt = 1_750_000_000_000L,
            periods = listOf(
                WatchPeriod("M", "101", "AB", 800, 845, "normal", ""),
                WatchPeriod("En", "204", "CD", 850, 935, "irregular", "Covered by C. Diaz"),
                WatchPeriod("PE", "GYM", "EF", 955, 1040, "cancelled", "Sports day postponed"),
            ),
        )
        val encoded = WatchPayloadCodec.encode(payload)
        assertTrue(encoded.size < 1024)
        val decoded = WatchPayloadCodec.decode(encoded)
        assertEquals(payload, decoded)
        val again = WatchPayloadCodec.encode(decoded)
        assertTrue(encoded.contentEquals(again))
    }
}
