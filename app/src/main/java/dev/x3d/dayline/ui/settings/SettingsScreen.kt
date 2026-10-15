package dev.x3d.dayline.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.ui.components.ScreenTopBar
import dev.x3d.dayline.ui.components.SectionLabel
import dev.x3d.dayline.ui.components.SegmentedGroup
import java.text.DateFormat
import java.util.Date
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    val time = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(title = stringResource(R.string.settings_title))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_account))
            SegmentedGroup(
                listOf(
                    { SettingRow(Icons.Outlined.School, state.session?.schoolDisplayName ?: "—", state.session?.host) },
                    {
                        SettingRow(
                            Icons.Outlined.AccountCircle,
                            state.session?.user ?: "—",
                            state.session?.let { stringResource(accountTypeLabel(it)) },
                        )
                    },
                ),
            )

            SectionLabel(stringResource(R.string.settings_appearance))
            AppearanceGroup(state, viewModel)

            SectionLabel(stringResource(R.string.settings_sync))
            SegmentedGroup(
                listOf(
                    {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.settings_sync_interval),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Connected button group (M3 Expressive).
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                val options = listOf(15 to R.string.settings_sync_15, 30 to R.string.settings_sync_30, 60 to R.string.settings_sync_60)
                                options.forEachIndexed { index, (minutes, label) ->
                                    ToggleButton(
                                        checked = state.interval == minutes,
                                        onCheckedChange = { viewModel.setInterval(minutes) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) {
                                        Text(stringResource(label), maxLines = 1)
                                    }
                                }
                            }
                        }
                    },
                    {
                        SettingRow(
                            Icons.Outlined.Sync,
                            state.sync.lastSuccessAt?.let { stringResource(R.string.settings_last_sync, time.format(Date(it))) }
                                ?: stringResource(R.string.settings_never_synced),
                            state.sync.lastError,
                        ) {
                            FilledTonalButton(onClick = viewModel::syncNow, shapes = ButtonDefaults.shapes(), enabled = !state.sync.isSyncing) {
                                Text(stringResource(R.string.settings_sync_now))
                            }
                        }
                    },
                ),
            )

            SectionLabel(stringResource(R.string.settings_watch))
            SegmentedGroup(
                listOf(
                    {
                        val pushed = state.watch.lastPushedAt
                        SettingRow(
                            Icons.Outlined.Watch,
                            if (pushed == null) stringResource(R.string.settings_watch_none) else stringResource(R.string.settings_watch_ok, time.format(Date(pushed))),
                            state.watch.message ?: stringResource(R.string.settings_watch_hint),
                        )
                    },
                ),
            )

            SectionLabel(stringResource(R.string.settings_privacy_title))
            SegmentedGroup(listOf({ SettingRow(Icons.Outlined.Lock, stringResource(R.string.settings_privacy), null) }))

            Spacer(Modifier.padding(4.dp))
            FilledTonalButton(
                onClick = { confirmLogout = true },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_logout))
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout(onLoggedOut)
                }) { Text(stringResource(R.string.settings_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private fun accountTypeLabel(session: UserSession): Int = when (session.personType) {
    UserSession.PERSON_STUDENT -> R.string.account_student
    UserSession.PERSON_TEACHER -> R.string.account_teacher
    UserSession.PERSON_PARENT -> R.string.account_parent
    else -> R.string.account_other
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    supporting: String?,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}
