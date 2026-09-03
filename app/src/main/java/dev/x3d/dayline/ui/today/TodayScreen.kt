package dev.x3d.dayline.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.time.minutesUntil
import dev.x3d.dayline.ui.components.LessonCard
import dev.x3d.dayline.ui.components.OfflineBanner
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onLesson: (Long) -> Unit,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PullToRefreshBox(isRefreshing = state.sync.isSyncing, onRefresh = viewModel::refresh) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.today_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
            item { OfflineBanner(state.sync.offline || state.sync.fromCache && state.sync.lastError != null) }
            if (state.lessons.isEmpty() && !state.sync.isSyncing) {
                item {
                    EmptyState(stringResource(R.string.today_empty))
                }
            }
            state.next?.let { next ->
                item {
                    NextHero(next, state.now, Modifier.padding(horizontal = 20.dp), onClick = { onLesson(next.id) })
                }
            }
            items(state.lessons, key = { it.id }) { lesson ->
                TimelineRow(
                    lesson = lesson,
                    showNow = state.next?.id == lesson.id,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = { onLesson(lesson.id) },
                )
            }
        }
    }
}

@Composable
private fun NextHero(lesson: Lesson, now: dev.x3d.dayline.domain.time.UntisTime, modifier: Modifier, onClick: () -> Unit) {
    val minsToStart = minutesUntil(lesson.start, now)
    val minsToEnd = minutesUntil(lesson.end, now)
    val countdown = when {
        minsToStart > 0 -> stringResource(R.string.starts_in, minsToStart)
        minsToEnd > 0 -> stringResource(R.string.ends_in, minsToEnd)
        else -> stringResource(R.string.today_done)
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.today_next), style = MaterialTheme.typography.labelLarge)
            Text(lesson.subjectLabel, style = MaterialTheme.typography.displaySmall)
            Text("${lesson.roomLabel} · ${lesson.start.format()}", style = MaterialTheme.typography.titleMedium)
            Text(countdown, style = MaterialTheme.typography.titleLarge)
            lesson.substText?.let { Text(it) }
        }
    }
}

@Composable
private fun TimelineRow(lesson: Lesson, showNow: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(
                        if (showNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
            Box(
                Modifier
                    .width(2.dp)
                    .height(88.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            )
        }
        Spacer(Modifier.width(12.dp))
        LessonCard(lesson = lesson, onClick = onClick)
    }
}

@Composable
fun EmptyState(text: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
