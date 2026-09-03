package dev.x3d.dayline.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onScanQr: () -> Unit,
    onChangeSchool: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.consumeQr() }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
            Text(state.schoolLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = state.user,
                onValueChange = viewModel::onUser,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_user)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.useSecret,
                    onClick = { viewModel.setUseSecret(false) },
                    label = { Text(stringResource(R.string.login_use_password)) },
                )
                FilterChip(
                    selected = state.useSecret,
                    onClick = { viewModel.setUseSecret(true) },
                    label = { Text(stringResource(R.string.login_use_secret)) },
                )
            }
            if (state.useSecret) {
                OutlinedTextField(
                    value = state.secret,
                    onValueChange = viewModel::onSecret,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_secret)) },
                    placeholder = { Text(stringResource(R.string.login_secret_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.login_scan_qr))
                }
            } else {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.large,
                )
            }
            if (state.error) {
                Text(stringResource(R.string.login_failed), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.submit(onLoggedIn) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading && state.user.isNotBlank(),
                shape = MaterialTheme.shapes.large,
            ) {
                if (state.loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(R.string.login_action))
            }
            TextButton(onClick = onChangeSchool) { Text(stringResource(R.string.change_school)) }
        }
    }
}
