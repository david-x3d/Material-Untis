package dev.x3d.dayline.ui.absences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.model.Absence
import dev.x3d.dayline.domain.model.AbsenceStatus
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.ui.components.ScreenTopBar
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AbsencesScreen(viewModel: AbsencesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.absences_title),
            subtitle = state.report?.schoolYear?.let { year -> "${formatDate(year.start)} – ${formatDate(year.end)}" },
        )
        val pullState = rememberPullToRefreshState()
        val refreshing = state.loading && state.report != null
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { error ->
                    item { ErrorCard(error, onRetry = viewModel::refresh) }
                }
                if (state.report == null && state.loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                }
                if (state.report != null) {
                    item { FilterBar(state, viewModel::setFilter) }
                    if (state.visible.isEmpty()) {
                        item {
                            Text(
                                stringResource(if (state.all.isEmpty()) R.string.absences_empty else R.string.absences_filter_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                            )
                        }
                    }
                    items(state.visible, key = { it.id to it.startDate.yyyymmdd }) { absence ->
                        AbsenceCard(absence)
                    }
                }
            }
        }
    }
}

private fun AbsenceFilter.label(): Int = when (this) {
    AbsenceFilter.ALL -> R.string.absences_filter_all
    AbsenceFilter.EXCUSED -> R.string.absence_excused
    AbsenceFilter.OPEN -> R.string.absence_open
}

/** Connected button group: filter and count in one (replaces the stat tiles). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterBar(state: AbsencesUiState, onFilter: (AbsenceFilter) -> Unit) {
    val options = AbsenceFilter.entries
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, filter ->
            ToggleButton(
                checked = state.filter == filter,
                onCheckedChange = { onFilter(filter) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Text(
                    "${stringResource(filter.label())} ${state.count(filter)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AbsenceCard(absence: Absence) {
    val icon = when (absence.status) {
        AbsenceStatus.EXCUSED -> Icons.Outlined.CheckCircle
        AbsenceStatus.UNEXCUSED -> Icons.Outlined.Close
        AbsenceStatus.OPEN -> Icons.Outlined.HourglassEmpty
    }
    val tint = when (absence.status) {
        AbsenceStatus.EXCUSED -> MaterialTheme.colorScheme.primary
        AbsenceStatus.UNEXCUSED -> MaterialTheme.colorScheme.error
        AbsenceStatus.OPEN -> MaterialTheme.colorScheme.tertiary
    }
    val fallback = when (absence.status) {
        AbsenceStatus.EXCUSED -> R.string.absence_excused
        AbsenceStatus.UNEXCUSED -> R.string.absence_unexcused
        AbsenceStatus.OPEN -> R.string.absence_open
    }
    // Same look as a timetable block: tonal surface with a coloured stripe.
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(tint, CircleShape),
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(dateRange(absence), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Line(Icons.Outlined.Schedule, "${absence.start.format()} – ${absence.end.format()}")
                absence.reason?.let { Line(Icons.Outlined.Info, it) }
                absence.text?.let { Line(Icons.AutoMirrored.Outlined.Notes, it) }
                Line(icon, absence.statusLabel ?: stringResource(fallback), tint)
            }
        }
    }
}

@Composable
private fun Line(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorCard(error: Throwable, onRetry: () -> Unit) {
    val message = when (error) {
        is PeriodException.Network -> stringResource(R.string.absences_error_offline)
        is PeriodException.SessionExpired, is PeriodException.Auth -> stringResource(R.string.absences_error_auth)
        else -> stringResource(R.string.absences_error, error.message ?: error.javaClass.simpleName)
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.error_retry)) }
        }
    }
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatDate(date: UntisDate): String = date.toLocalDate().format(DAY_FORMAT)

private fun dateRange(absence: Absence): String =
    if (absence.startDate == absence.endDate) {
        absence.startDate.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, ")) + formatDate(absence.startDate)
    } else {
        "${formatDate(absence.startDate)} – ${formatDate(absence.endDate)}"
    }
