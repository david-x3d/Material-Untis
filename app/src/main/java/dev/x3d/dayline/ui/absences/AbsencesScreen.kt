package dev.x3d.dayline.ui.absences

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AbsencesScreen(viewModel: AbsencesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filterOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.absences_title), style = MaterialTheme.typography.titleLarge)
                    state.report?.schoolYear?.let { year ->
                        Text(
                            "${formatDate(year.start)} – ${formatDate(year.end)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { filterOpen = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.absences_filter))
                    }
                    DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                        AbsenceFilter.entries.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(stringResource(filter.label())) },
                                onClick = {
                                    viewModel.setFilter(filter)
                                    filterOpen = false
                                },
                                trailingIcon = {
                                    if (state.filter == filter) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                },
                            )
                        }
                    }
                }
            },
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
                    item { Summary(state.all) }
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
    AbsenceFilter.UNEXCUSED -> R.string.absence_unexcused
    AbsenceFilter.OPEN -> R.string.absence_open
}

@Composable
private fun Summary(all: List<Absence>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            stringResource(R.string.absences_total),
            all.size,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(R.string.absence_excused),
            all.count { it.status == AbsenceStatus.EXCUSED },
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(R.string.absences_not_excused),
            all.count { it.status != AbsenceStatus.EXCUSED },
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, container: Color, content: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun AbsenceCard(absence: Absence) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(dateRange(absence), style = MaterialTheme.typography.titleLarge)
            Line(Icons.Outlined.Schedule, "${absence.start.format()} – ${absence.end.format()}")
            absence.reason?.let { Line(Icons.Outlined.Info, it) }
            absence.text?.let { Line(Icons.AutoMirrored.Outlined.Notes, it) }
            val (icon, tint, fallback) = when (absence.status) {
                AbsenceStatus.EXCUSED -> Triple(Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.primary, R.string.absence_excused)
                AbsenceStatus.UNEXCUSED -> Triple(Icons.Outlined.Close, MaterialTheme.colorScheme.error, R.string.absence_unexcused)
                AbsenceStatus.OPEN -> Triple(Icons.Outlined.HourglassEmpty, MaterialTheme.colorScheme.tertiary, R.string.absence_open)
            }
            Line(icon, absence.statusLabel ?: stringResource(fallback), tint)
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
