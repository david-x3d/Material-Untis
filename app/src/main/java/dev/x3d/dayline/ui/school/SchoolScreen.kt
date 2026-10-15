package dev.x3d.dayline.ui.school

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.ui.onboarding.StepHeader
import org.koin.androidx.compose.koinViewModel

/** Onboarding step 2: find the school (search, or server + school name by hand). */
@Composable
fun SchoolScreen(
    onContinue: () -> Unit,
    viewModel: SchoolViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var manual by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                StepHeader(2, stringResource(R.string.school_title), stringResource(R.string.school_hint))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.school_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                )
            }
        }
        if (state.searching) {
            item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        }
        if (state.error != null) {
            item { Text(stringResource(R.string.school_search_error), color = MaterialTheme.colorScheme.error) }
        }
        itemsIndexed(state.results, key = { _, it -> it.host + it.loginName }) { index, school ->
            val big = 24.dp
            val small = 6.dp
            Surface(
                onClick = { viewModel.select(school, onContinue) },
                shape = RoundedCornerShape(
                    topStart = if (index == 0) big else small,
                    topEnd = if (index == 0) big else small,
                    bottomStart = if (index == state.results.lastIndex) big else small,
                    bottomEnd = if (index == state.results.lastIndex) big else small,
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(school.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOf(school.address, school.loginName).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (!state.searching && state.query.trim().length >= 2 && state.results.isEmpty() && state.error == null) {
            item { Text(stringResource(R.string.school_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { manual = !manual }) {
                    Icon(Icons.Outlined.Dns, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.school_manual))
                }
                AnimatedVisibility(manual) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.host,
                            onValueChange = viewModel::onHost,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.school_host_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                        )
                        OutlinedTextField(
                            value = state.schoolName,
                            onValueChange = viewModel::onSchoolName,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.school_login_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                        )
                        Button(
                            onClick = { viewModel.saveManual(onContinue) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.host.isNotBlank() && state.schoolName.isNotBlank(),
                        ) {
                            Text(stringResource(R.string.school_continue))
                        }
                    }
                }
            }
        }
    }
}
