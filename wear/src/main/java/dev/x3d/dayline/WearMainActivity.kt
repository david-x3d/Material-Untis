package dev.x3d.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.x3d.dayline.data.PhoneSync
import dev.x3d.dayline.data.WearTimetableStore
import dev.x3d.dayline.domain.model.WatchPayload
import dev.x3d.dayline.domain.model.WatchPeriod
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import dev.x3d.dayline.domain.time.minutesUntil
import java.text.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class WearMainActivity : ComponentActivity() {
    private val store: WearTimetableStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val colors = remember { dynamicColorScheme(context) }
            MaterialTheme(colorScheme = colors ?: MaterialTheme.colorScheme) {
                AppScaffold {
                    TodayScreen(store)
                }
            }
        }
    }
}

/** A run of identical back-to-back periods (double lessons), like on the phone. */
private data class WatchBlock(val periods: List<WatchPeriod>) {
    val first get() = periods.first()
    val start get() = periods.first().start
    val end get() = periods.last().end
    val cancelled get() = first.status == "cancelled"
    val irregular get() = first.status == "irregular"
}

private fun merge(periods: List<WatchPeriod>): List<WatchBlock> {
    val blocks = mutableListOf<MutableList<WatchPeriod>>()
    for (p in periods.sortedBy { it.start }) {
        val last = blocks.lastOrNull()?.last()
        val joins = last != null &&
            last.subject == p.subject && last.room == p.room && last.teacher == p.teacher &&
            last.status == p.status && last.info == p.info &&
            UntisTime(p.start).toMinutesOfDay() - UntisTime(last.end).toMinutesOfDay() in 0..5
        if (joins) blocks.last() += p else blocks += mutableListOf(p)
    }
    return blocks.map { WatchBlock(it) }
}

private data class DayState(val blocks: List<WatchBlock>, val current: WatchBlock?, val isToday: Boolean)

private fun dayState(payload: WatchPayload?, today: UntisDate, now: UntisTime): DayState {
    if (payload == null) return DayState(emptyList(), null, false)
    val blocks = merge(payload.periods)
    val isToday = payload.date == today.yyyymmdd
    val active = blocks.filterNot { it.cancelled }
    val current = if (!isToday) {
        null
    } else {
        active.firstOrNull { now.raw in it.start until it.end } ?: active.firstOrNull { it.start > now.raw }
    }
    return DayState(blocks, current, isToday)
}

@Composable
private fun TodayScreen(store: WearTimetableStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val payload by store.payload.collectAsStateWithLifecycle(initialValue = null)
    // One clock tick per 30 s; everything else is derived and remembered, so scrolling stays cheap.
    val clock by produceState(UntisDate.today() to UntisTime.now()) {
        while (true) {
            delay(30_000)
            value = UntisDate.today() to UntisTime.now()
        }
    }
    val (today, now) = clock
    val state = remember(payload, today, now) { dayState(payload, today, now) }
    var syncing by remember { mutableStateOf(false) }
    var noPhone by remember { mutableStateOf(false) }

    fun requestSync() {
        if (syncing) return
        scope.launch {
            syncing = true
            noPhone = runCatching { PhoneSync.request(context) }.getOrDefault(0) == 0
            delay(2_500)
            syncing = false
        }
    }

    // Ask the phone for a fresh plan when the app opens without today's data.
    LaunchedEffect(state.isToday) {
        if (!state.isToday) requestSync()
    }

    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = ::requestSync,
                buttonSize = EdgeButtonSize.ExtraSmall,
                enabled = !syncing,
            ) {
                Text(stringResource(if (syncing) R.string.wear_syncing else R.string.wear_sync))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "header") {
                ListHeader(Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(
                        if (state.isToday) {
                            today.toLocalDate().format(DateTimeFormatter.ofPattern("EEE d. MMM"))
                        } else {
                            stringResource(R.string.app_name)
                        },
                    )
                }
            }
            when {
                payload == null || !state.isToday -> item(key = "empty") {
                    Message(
                        stringResource(if (noPhone) R.string.wear_no_phone else R.string.wear_empty),
                        Modifier.transformedHeight(this, spec),
                    )
                }
                state.blocks.isEmpty() -> item(key = "free") {
                    Message(stringResource(R.string.wear_free), Modifier.transformedHeight(this, spec))
                }
                else -> {
                    val current = state.current
                    if (current != null) {
                        item(key = "hero") {
                            HeroCard(current, now, Modifier.transformedHeight(this, spec), SurfaceTransformation(spec))
                        }
                    } else {
                        item(key = "done") {
                            Message(stringResource(R.string.wear_done), Modifier.transformedHeight(this, spec))
                        }
                    }
                    items(state.blocks, key = { it.start }) { block ->
                        BlockCard(
                            block,
                            past = block.end <= now.raw,
                            modifier = Modifier.transformedHeight(this, spec),
                            transformation = SurfaceTransformation(spec),
                        )
                    }
                }
            }
            payload?.let { p ->
                item(key = "synced") {
                    Text(
                        stringResource(R.string.wear_synced, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(p.syncedAt))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).transformedHeight(this, spec),
                    )
                }
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Big card for the lesson running now (or the next one), with a countdown. */
@Composable
private fun HeroCard(block: WatchBlock, now: UntisTime, modifier: Modifier, transformation: SurfaceTransformation) {
    val start = UntisTime(block.start)
    val end = UntisTime(block.end)
    val toStart = minutesUntil(start, now)
    val label = if (toStart > 0) {
        stringResource(R.string.wear_starts_in, toStart)
    } else {
        stringResource(R.string.wear_ends_in, minutesUntil(end, now))
    }
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        transformation = transformation,
    ) {
        Text(
            stringResource(if (toStart > 0) R.string.wear_next else R.string.wear_now),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            block.first.subject,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(block.first.room, block.first.teacher).filter { it.isNotBlank() && it != "—" }.joinToString(" · "),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
        if (block.first.info.isNotBlank()) {
            Text(block.first.info, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Compact row like a timetable block: coloured stripe, subject, room · teacher, time. */
@Composable
private fun BlockCard(block: WatchBlock, past: Boolean, modifier: Modifier, transformation: SurfaceTransformation) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (block.cancelled) colors.errorContainer.copy(alpha = 0.6f) else colors.surfaceContainer,
            contentColor = if (block.cancelled) colors.onErrorContainer else colors.onSurface,
        ),
        contentPadding = PaddingValues(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        transformation = transformation,
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (block.irregular) colors.primary else subjectColor(block.first.subject), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    block.first.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (block.cancelled) TextDecoration.LineThrough else null,
                    color = if (past && !block.cancelled) colors.onSurfaceVariant else Color.Unspecified,
                    maxLines = 1,
                )
                Text(
                    listOf(block.first.room, block.first.teacher).filter { it.isNotBlank() && it != "—" }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (block.irregular) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(UntisTime(block.start).format(), style = MaterialTheme.typography.labelMedium)
                Text(UntisTime(block.end).format(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Same hue per subject as on the phone. */
private fun subjectColor(subject: String): Color {
    if (subject.isEmpty()) return Color.Gray
    val hue = (abs(subject.hashCode()) % 360).toFloat()
    return Color.hsl(hue, saturation = 0.55f, lightness = 0.72f)
}
