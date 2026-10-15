package dev.x3d.dayline.ui.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.LessonType
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import dev.x3d.dayline.ui.components.ScreenTopBar
import dev.x3d.dayline.ui.theme.subjectColor
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val CENTER_PAGE = 520
private val TIME_COLUMN = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onLesson: (Long) -> Unit,
    viewModel: TimetableViewModel = koinViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val pager = rememberPagerState(initialPage = CENTER_PAGE) { CENTER_PAGE * 2 }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val today by produceState(UntisDate.today()) {
        while (true) {
            delay(30_000)
            value = UntisDate.today()
        }
    }

    LaunchedEffect(pager.settledPage) {
        viewModel.ensureLoaded(viewModel.mondayFor(pager.settledPage - CENTER_PAGE))
    }
    LaunchedEffect(Unit) {
        viewModel.errors.collect { error ->
            val message = when (error) {
                is PeriodException.Network -> context.getString(R.string.offline_banner)
                is PeriodException.SessionExpired -> context.getString(R.string.login_failed)
                else -> error.message ?: context.getString(R.string.error_generic)
            }
            snackbar.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = session?.schoolDisplayName ?: stringResource(R.string.nav_timetable),
                actions = {
                    if (pager.currentPage != CENTER_PAGE) {
                        FilledTonalIconButton(onClick = { scope.launch { pager.animateScrollToPage(CENTER_PAGE) } }) {
                            Icon(Icons.Outlined.Today, contentDescription = stringResource(R.string.timetable_this_week))
                        }
                    }
                },
            )
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                val monday = remember(page) { viewModel.mondayFor(page - CENTER_PAGE) }
                val weekFlow = remember(monday) { viewModel.week(monday) }
                val week by weekFlow.collectAsStateWithLifecycle(initialValue = null)
                WeekPage(
                    monday = monday,
                    week = week,
                    today = today,
                    refreshing = monday in loading,
                    onRefresh = { viewModel.refresh(monday) },
                    onLesson = onLesson,
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekPage(
    monday: UntisDate,
    week: WeekData?,
    today: UntisDate,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onLesson: (Long) -> Unit,
) {
    val days = week?.days ?: (0L..4L).map { monday.plusDays(it) }
    Column(Modifier.fillMaxSize()) {
        WeekHeader(monday, days, today)
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            if (week != null && week.isEmpty && !refreshing) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.timetable_week_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (week != null) {
                WeekGrid(week, today, onLesson)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekHeader(monday: UntisDate, days: List<UntisDate>, today: UntisDate) {
    val locale = Locale.getDefault()
    val mondayDate = monday.toLocalDate()
    val todayShape = remember { MaterialShapes.Cookie9Sided }.toShape()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(TIME_COLUMN).padding(start = 10.dp)) {
            Text(
                stringResource(R.string.timetable_week_number, mondayDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                mondayDate.month.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        days.forEach { day ->
            val date = day.toLocalDate()
            val isToday = day == today
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).removeSuffix("."),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, todayShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGrid(week: WeekData, today: UntisDate, onLesson: (Long) -> Unit) {
    val axis = week.axis
    val now by produceState(UntisTime.now()) {
        while (true) {
            delay(30_000)
            value = UntisTime.now()
        }
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Row(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TimeColumn(axis)
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(axis.totalHeight.dp)
                .padding(end = 4.dp),
        ) {
            val columnWidth = maxWidth / week.days.size
            Canvas(Modifier.fillMaxSize()) {
                axis.slots.indices.forEach { index ->
                    val y = axis.top(index).dp.toPx()
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            week.days.forEachIndexed { dayIndex, day ->
                val isToday = day == today
                if (isToday) {
                    Box(
                        Modifier
                            .offset(x = columnWidth * dayIndex)
                            .width(columnWidth)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
                    )
                }
                week.blocksByDay[day].orEmpty().forEach { placed ->
                    val laneWidth = columnWidth / placed.lanes
                    val top = axis.y(placed.block.start)
                    val bottom = axis.y(placed.block.end)
                    BlockCell(
                        placed = placed,
                        modifier = Modifier
                            .offset(x = columnWidth * dayIndex + laneWidth * placed.lane, y = top.dp)
                            .width(laneWidth)
                            .height((bottom - top).coerceAtLeast(24f).dp)
                            .padding(2.dp),
                        onClick = { onLesson(placed.block.first.id) },
                    )
                }
                if (isToday && axis.contains(now)) {
                    NowLine(
                        Modifier
                            .offset(x = columnWidth * dayIndex, y = axis.y(now).dp - 5.dp)
                            .width(columnWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(axis: TimeAxis) {
    Box(
        Modifier
            .width(TIME_COLUMN)
            .height(axis.totalHeight.dp),
    ) {
        axis.slots.forEachIndexed { index, slot ->
            TimeLabel(slot.start, Modifier.offset(y = axis.top(index).dp - 2.dp))
            val next = axis.slots.getOrNull(index + 1)
            if (next == null || next.start != slot.end) {
                TimeLabel(slot.end, Modifier.offset(y = axis.bottom(index).dp - 16.dp))
            }
        }
    }
}

@Composable
private fun TimeLabel(time: UntisTime, modifier: Modifier) {
    Text(
        time.format(),
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun NowLine(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.error
    Row(modifier.height(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Box(Modifier.weight(1f).height(2.dp).background(color))
    }
}

@Composable
private fun BlockCell(placed: PlacedBlock, modifier: Modifier, onClick: () -> Unit) {
    val lesson = placed.block.first
    val colors = MaterialTheme.colorScheme
    val cancelled = lesson.status == LessonStatus.CANCELLED
    val irregular = lesson.status == LessonStatus.IRREGULAR
    val exam = lesson.type == LessonType.EXAM
    val container = when {
        cancelled -> colors.errorContainer.copy(alpha = 0.55f)
        exam -> colors.tertiaryContainer
        else -> colors.surfaceContainerHigh
    }
    val content = when {
        cancelled -> colors.onErrorContainer
        exam -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
    val hasNote = placed.block.lessons.any { it.info != null || it.substText != null || it.lessonText != null }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        contentColor = content,
        border = if (irregular) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Box {
            Row {
                Box(
                    Modifier
                        .padding(start = 4.dp, top = 6.dp, bottom = 6.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(subjectColor(lesson.subject?.shortName.orEmpty()), CircleShape),
                )
                Column(
                    Modifier
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                        .alpha(if (cancelled) 0.8f else 1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        lesson.subjectShort,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    RefText(lesson.room)
                    RefText(lesson.teacher)
                    if (placed.lanes == 1) lesson.klass?.takeIf { lesson.type != LessonType.LESSON }?.let { RefText(it) }
                }
            }
            if (hasNote) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(12.dp),
                    tint = content.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** Short name of a room/teacher. Replaced values (substitutions) are highlighted like in Untis. */
@Composable
private fun RefText(ref: NamedRef?) {
    if (ref == null) return
    val changed = ref.originalShortName != null && ref.originalShortName != ref.shortName
    val style = MaterialTheme.typography.bodySmall
    if (changed) {
        Text(
            ref.shortName,
            style = style,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 0.dp),
        )
    } else {
        Text(ref.shortName, style = style, maxLines = 1, overflow = TextOverflow.Clip)
    }
}
