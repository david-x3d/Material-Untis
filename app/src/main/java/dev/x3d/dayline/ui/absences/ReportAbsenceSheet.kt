package dev.x3d.dayline.ui.absences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.model.AbsenceReason
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "Krankmeldung": the same report the WebUntis web app sends. Asks for confirmation before sending. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReportAbsenceSheet(
    form: ReportForm,
    onChange: ((ReportForm) -> ReportForm) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pickDate by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end
    var pickTime by remember { mutableStateOf<Boolean?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val options = form.options

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineSmall)

            when {
                form.loadingOptions -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                options != null && !options.canReport -> Text(
                    stringResource(R.string.report_not_allowed),
                    color = MaterialTheme.colorScheme.error,
                )
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton(stringResource(R.string.report_from), form.startDate, Modifier.weight(1f)) { pickDate = true }
                        DateButton(stringResource(R.string.report_to), form.endDate, Modifier.weight(1f)) { pickDate = false }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.report_whole_day), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${form.startTime.format()} – ${form.endTime.format()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = form.wholeDay, onCheckedChange = { checked -> onChange { it.copy(wholeDay = checked) } })
                    }
                    if (!form.wholeDay) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeButton(stringResource(R.string.report_from), form.startTime, Modifier.weight(1f)) { pickTime = true }
                            TimeButton(stringResource(R.string.report_to), form.endTime, Modifier.weight(1f)) { pickTime = false }
                        }
                    }
                    if (options != null && !options.reasonAndTextHidden) {
                        ReasonField(form, options.reasons, onChange)
                        OutlinedTextField(
                            value = form.text,
                            onValueChange = { value -> onChange { it.copy(text = value) } },
                            label = { Text(stringResource(R.string.report_text)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                    }
                }
            }

            form.error?.let { error ->
                Text(
                    when (error) {
                        is PeriodException.Network -> stringResource(R.string.absences_error_offline)
                        is PeriodException.SessionExpired, is PeriodException.Auth -> stringResource(R.string.report_error_auth)
                        else -> stringResource(R.string.report_error, error.message ?: error.javaClass.simpleName)
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }

            val valid = form.reasonId != null && options?.canReport == true &&
                (form.endDate > form.startDate || form.endDate == form.startDate && form.endTime > form.startTime)
            Button(
                onClick = { confirm = true },
                enabled = valid && !form.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.sending) {
                    LoadingIndicator(Modifier.padding(end = 8.dp))
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Text(stringResource(R.string.report_send))
            }
        }
    }

    pickDate?.let { start ->
        val initial = (if (start) form.startDate else form.endDate).toLocalDate()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = UntisDate.from(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        onChange {
                            if (start) it.copy(startDate = date, endDate = maxOf(date, it.endDate)) else it.copy(endDate = maxOf(date, it.startDate))
                        }
                    }
                    pickDate = null
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = null }) { Text(stringResource(android.R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }

    pickTime?.let { start ->
        val initial = if (start) form.startTime else form.endTime
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = null },
            confirmButton = {
                TextButton(onClick = {
                    val time = UntisTime(state.hour * 100 + state.minute)
                    onChange { if (start) it.copy(startTime = time) else it.copy(endTime = time) }
                    pickTime = null
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = null }) { Text(stringResource(android.R.string.cancel)) } },
            text = { TimePicker(state = state) },
        )
    }

    if (confirm) {
        val reason = options?.reasons?.firstOrNull { it.id == form.reasonId }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.report_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.report_confirm,
                        formatDate(form.startDate) + " " + form.startTime.format(),
                        formatDate(form.endDate) + " " + form.endTime.format(),
                        reason,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onSend()
                }) { Text(stringResource(R.string.report_send)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonField(form: ReportForm, reasons: List<AbsenceReason>, onChange: ((ReportForm) -> ReportForm) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it && reasons.size > 1 }) {
        OutlinedTextField(
            value = reasons.firstOrNull { it.id == form.reasonId }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.report_reason)) },
            trailingIcon = { if (reasons.size > 1) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            reasons.forEach { reason ->
                DropdownMenuItem(
                    text = { Text(reason.name) },
                    onClick = {
                        onChange { it.copy(reasonId = reason.id) }
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DateButton(label: String, date: UntisDate, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(formatDate(date), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TimeButton(label: String, time: UntisTime, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(time.format(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun formatDate(date: UntisDate): String =
    date.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

