package dev.x3d.dayline.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import java.text.DateFormat
import java.util.Date
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.settings_account), style = MaterialTheme.typography.titleMedium)
            Text(state.session?.schoolDisplayName ?: "—", style = MaterialTheme.typography.headlineMedium)
            Text(state.session?.user ?: "—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_sync), style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntervalChip(15, state.interval, R.string.settings_sync_15, viewModel::setInterval)
                IntervalChip(30, state.interval, R.string.settings_sync_30, viewModel::setInterval)
                IntervalChip(60, state.interval, R.string.settings_sync_60, viewModel::setInterval)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.settings_watch), style = MaterialTheme.typography.titleMedium)
            val pushed = state.watch.lastPushedAt
            if (pushed == null) {
                Text(stringResource(R.string.settings_watch_unknown), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val formatted = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(pushed))
                Text(stringResource(R.string.settings_watch_ok, formatted))
            }
        }
        Text(stringResource(R.string.settings_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(
            onClick = { viewModel.logout(onLoggedOut) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.settings_logout))
        }
    }
}

@Composable
private fun IntervalChip(value: Int, selected: Int, label: Int, onClick: (Int) -> Unit) {
    FilterChip(
        selected = selected == value,
        onClick = { onClick(value) },
        label = { Text(stringResource(label)) },
    )
}
