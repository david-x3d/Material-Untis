package dev.x3d.dayline.ui.school

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun SchoolScreen(
    onContinue: () -> Unit,
    viewModel: SchoolViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.school_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.school_search_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
            }
            if (state.searching) {
                item { CircularProgressIndicator() }
            }
            if (state.error != null) {
                item {
                    Text(
                        stringResource(R.string.school_search_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(state.results, key = { it.host + it.loginName }) { school ->
                ListItem(
                    headlineContent = { Text(school.displayName) },
                    supportingContent = { Text(listOf(school.loginName, school.address).filter { it.isNotBlank() }.joinToString(" · ")) },
                    modifier = Modifier.clickable { viewModel.select(school, onContinue) },
                )
            }
            if (!state.searching && state.query.length >= 2 && state.results.isEmpty() && state.error == null) {
                item { Text(stringResource(R.string.school_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.school_manual), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.host,
                    onValueChange = viewModel::onHost,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.school_host_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.schoolName,
                    onValueChange = viewModel::onSchoolName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.school_login_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveManual(onContinue) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.host.isNotBlank() && state.schoolName.isNotBlank(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.school_continue))
                }
            }
        }
    }
}
