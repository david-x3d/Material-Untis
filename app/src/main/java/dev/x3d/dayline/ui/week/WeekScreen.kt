package dev.x3d.dayline.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.ui.components.LessonCard
import dev.x3d.dayline.ui.today.EmptyState
import java.time.LocalDate
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun WeekScreen(
    onLesson: (Long) -> Unit,
    viewModel: WeekViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val todayIndex = state.days.indexOf(UntisDate.today()).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = todayIndex) { state.days.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.week_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        if (state.days.isNotEmpty()) {
            ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 20.dp) {
                state.days.forEachIndexed { index, day ->
                    Tab(
                        selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        text = {
                            Column {
                                Text(day.formatWeekday())
                                Text(day.formatDisplay(), style = MaterialTheme.typography.labelLarge)
                            }
                        },
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val day = state.days[page]
                val lessons = state.lessonsByDate[day.yyyymmdd].orEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (lessons.isEmpty()) {
                        item { EmptyState(stringResource(R.string.week_empty)) }
                    }
                    items(lessons, key = { it.id }) { lesson ->
                        LessonCard(lesson = lesson, modifier = Modifier.fillMaxWidth(), onClick = { onLesson(lesson.id) })
                    }
                }
            }
        }
    }
}
