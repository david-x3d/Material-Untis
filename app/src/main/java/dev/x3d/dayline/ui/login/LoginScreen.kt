package dev.x3d.dayline.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.ui.onboarding.StepHeader
import org.koin.androidx.compose.koinViewModel

/** Onboarding step 3: sign in with password or the app secret from the Untis QR code. */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onScanQr: () -> Unit,
    onChangeSchool: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.consumeQr() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(
            3,
            stringResource(R.string.login_title),
            stringResource(if (state.parent) R.string.login_parent_hint else R.string.login_student_hint),
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                state.schoolLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = !state.useSecret,
                onCheckedChange = { viewModel.setUseSecret(false) },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Icon(Icons.Outlined.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.login_use_password))
            }
            ToggleButton(
                checked = state.useSecret,
                onCheckedChange = { viewModel.setUseSecret(true) },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.login_use_secret))
            }
        }
        OutlinedTextField(
            value = state.user,
            onValueChange = viewModel::onUser,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.login_user)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
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
            FilledTonalButton(onClick = onScanQr, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.large,
            )
        }
        if (state.error) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.login_failed),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.submit(onLoggedIn) },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !state.loading && state.user.isNotBlank(),
        ) {
            if (state.loading) {
                LoadingIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            } else {
                Text(stringResource(R.string.login_action), style = MaterialTheme.typography.titleMedium)
            }
        }
        TextButton(onClick = onChangeSchool) { Text(stringResource(R.string.change_school)) }
    }
}
