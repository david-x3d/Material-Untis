package dev.x3d.dayline.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.SubjectPalette
import dev.x3d.dayline.ui.theme.subjectColor

/** "Darstellung": wallpaper colours on/off, automatic (re-rollable) or hand-picked subject colours. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceGroup(state: SettingsUiState, viewModel: SettingsViewModel) {
    var picking by remember { mutableStateOf<NamedRef?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Surface(
                shape = MaterialTheme.shapes.large.copy(
                    bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                    bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd,
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.settings_dynamic_color_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        thumbContent = if (state.dynamicColor) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        Surface(
            shape = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MaterialTheme.shapes.large.copy(
                    topStart = MaterialTheme.shapes.extraSmall.topStart,
                    topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                )
            } else {
                MaterialTheme.shapes.large
            },
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_subject_colors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(
                        checked = !state.subjectColors.custom,
                        onCheckedChange = { viewModel.setCustomColors(false) },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_colors_auto))
                    }
                    ToggleButton(
                        checked = state.subjectColors.custom,
                        onCheckedChange = { viewModel.setCustomColors(true) },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_colors_custom))
                    }
                }
                if (!state.subjectColors.custom) {
                    FilledTonalButton(onClick = viewModel::reroll, shapes = ButtonDefaults.shapes()) {
                        Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_colors_reroll))
                    }
                } else {
                    Text(
                        stringResource(R.string.settings_colors_custom_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.subjects.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_colors_no_subjects),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.subjects.forEach { subject ->
                            SubjectChip(subject, onClick = { picking = subject })
                        }
                    }
                }
            }
        }
    }

    picking?.let { subject ->
        ColorSheet(
            subject = subject,
            selected = state.subjectColors.picked[subject.shortName]?.takeIf { state.subjectColors.custom },
            onPick = {
                viewModel.pickColor(subject.shortName, it)
                picking = null
            },
            onAuto = {
                viewModel.resetColor(subject.shortName)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun SubjectChip(subject: NamedRef, onClick: () -> Unit) {
    val color = subjectColor(subject.shortName)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(subject.shortName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ColorSheet(
    subject: NamedRef,
    selected: Int?,
    onPick: (Int) -> Unit,
    onAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(subject.longName, style = MaterialTheme.typography.headlineSmall)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4,
            ) {
                SubjectPalette.PRESETS.forEach { argb ->
                    val isSelected = argb == selected
                    val color = Color(argb)
                    Box(
                        Modifier
                            .weight(1f)
                            .size(56.dp)
                            .background(color, if (isSelected) cookie else CircleShape)
                            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, cookie) else Modifier)
                            .clickable { onPick(argb) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                            )
                        }
                    }
                }
            }
            OutlinedButton(onClick = onAuto, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_colors_use_auto))
            }
        }
    }
}
