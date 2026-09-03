package dev.x3d.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.x3d.dayline.data.WearTimetableStore
import dev.x3d.dayline.domain.model.WatchPeriod
import dev.x3d.dayline.domain.time.UntisTime
import org.koin.android.ext.android.inject

class WearMainActivity : ComponentActivity() {
    private val store: WearTimetableStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val payload by store.payload.collectAsStateWithLifecycle(initialValue = null)
            val now = UntisTime.now()
            val periods = payload?.periods.orEmpty()
            val next = periods.filter { it.status != "cancelled" }.firstOrNull { it.end > now.raw }
            MaterialTheme {
                ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ListHeader { Text(stringResource(R.string.wear_next)) }
                    }
                    if (periods.isEmpty()) {
                        item { Text(stringResource(R.string.wear_empty), modifier = Modifier.padding(12.dp)) }
                    } else if (next != null) {
                        item { PeriodCard(next, highlight = true) }
                    } else {
                        item { Text(stringResource(R.string.wear_done), modifier = Modifier.padding(12.dp)) }
                    }
                    items(periods) { period ->
                        if (period != next) PeriodCard(period, highlight = false)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PeriodCard(period: WatchPeriod, highlight: Boolean) {
    val cancelled = period.status == "cancelled"
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
    ) {
        Text(
            period.subject,
            style = MaterialTheme.typography.titleLarge,
            textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None,
        )
        Text("${UntisTime.parse(period.start).format()} · ${period.room}")
        if (period.status == "irregular" && period.info.isNotBlank()) {
            Text(period.info)
        }
        if (cancelled) {
            Text(stringResource(R.string.status_cancelled))
        }
    }
}
