package dev.x3d.dayline.complication

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.x3d.dayline.R
import dev.x3d.dayline.data.WearTimetableStore
import dev.x3d.dayline.domain.model.WatchPeriod
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

class NextLessonComplicationService : SuspendingComplicationDataSourceService() {
    private val store: WearTimetableStore by inject()

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return shortText("M · 101", "07:20")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val payload = store.payload.first()
        val now = UntisTime.now()
        val next = payload?.periods
            ?.filter { it.status != "cancelled" }
            ?.firstOrNull { it.end > now.raw }
        val text = if (next == null) {
            getString(R.string.wear_done)
        } else {
            "${next.subject} · ${next.room}"
        }
        val time = next?.let { UntisTime.parse(it.start).format() }.orEmpty()
        return when (request.complicationType) {
            ComplicationType.LONG_TEXT -> longText(text, time)
            else -> shortText(text, time)
        }
    }

    private fun shortText(text: String, title: String): ComplicationData =
        ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(getString(R.string.complication_label)).build(),
        ).setTitle(PlainComplicationText.Builder(title.ifBlank { " " }).build()).build()

    private fun longText(text: String, title: String): ComplicationData =
        LongTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(getString(R.string.complication_label)).build(),
        ).setTitle(PlainComplicationText.Builder(title.ifBlank { " " }).build()).build()
}
