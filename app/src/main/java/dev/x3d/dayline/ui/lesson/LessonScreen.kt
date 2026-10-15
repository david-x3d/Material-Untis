package dev.x3d.dayline.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.LessonType
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.ui.components.SegmentedGroup
import dev.x3d.dayline.ui.theme.subjectColor
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    id: Long,
    onBack: () -> Unit,
    viewModel: LessonViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { viewModel.setId(id) }
    val block by viewModel.block.collectAsStateWithLifecycle()
    val item = block
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (item != null) {
                    val date = item.first.date.toLocalDate()
                    Column {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEE, ")) +
                                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "${item.start.format()} – ${item.end.format()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
            },
        )
        if (item == null) return@Column
        val lesson = item.first
        val cancelled = lesson.status == LessonStatus.CANCELLED
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero: subject
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        cancelled -> MaterialTheme.colorScheme.errorContainer
                        lesson.type == LessonType.EXAM -> MaterialTheme.colorScheme.tertiaryContainer
                        lesson.status == LessonStatus.IRREGULAR -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(subjectColor(lesson.subject?.shortName.orEmpty()), MaterialTheme.shapes.large),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            lesson.subjectShort.take(3),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            lesson.subjectLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                        )
                        val status = when {
                            cancelled -> stringResource(R.string.status_cancelled)
                            lesson.type == LessonType.EXAM -> stringResource(R.string.status_exam)
                            lesson.status == LessonStatus.IRREGULAR -> stringResource(R.string.status_irregular)
                            else -> null
                        }
                        if (status != null) {
                            Surface(
                                shape = CircleShape,
                                color = if (cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    status,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Details, grouped like an expressive segmented list.
            val rows = buildList<@Composable () -> Unit> {
                add {
                    DetailRow(
                        Icons.Outlined.Schedule,
                        stringResource(R.string.lesson_time),
                        "${item.start.format()} – ${item.end.format()}",
                        if (item.lessons.size > 1) pluralStringResource(R.plurals.lesson_periods, item.lessons.size, item.lessons.size) else null,
                    )
                }
                lesson.room?.let { add { RefRow(Icons.Outlined.MeetingRoom, stringResource(R.string.lesson_room), it) } }
                lesson.teacher?.let { add { RefRow(Icons.Outlined.Person, stringResource(R.string.lesson_teacher), it) } }
                lesson.klass?.let { add { DetailRow(Icons.Outlined.Groups, stringResource(R.string.lesson_class), it.shortName, it.longName.takeIf { l -> l != it.shortName }) } }
            }
            SegmentedGroup(rows)

            val notes = listOfNotNull(
                lesson.substText?.let { stringResource(R.string.lesson_subst) to it },
                lesson.info?.let { stringResource(R.string.lesson_info) to it },
                lesson.lessonText?.let { stringResource(R.string.lesson_text) to it },
            )
            if (notes.isNotEmpty()) {
                SegmentedGroup(notes.map { (label, text) -> { DetailRow(Icons.Outlined.Info, label, text, null) } })
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, supporting: String?) {
    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "AB (Baker)", with the original value struck through when it was substituted. */
@Composable
private fun RefRow(icon: ImageVector, label: String, ref: NamedRef) {
    val original = ref.originalShortName?.takeIf { it != ref.shortName }
    if (original == null) {
        val long = ref.longName.takeIf { it.isNotBlank() && it != ref.shortName }
        DetailRow(icon, label, if (long != null) "${ref.shortName} ($long)" else ref.shortName, null)
        return
    }
    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(original)
                    }
                    append("  →  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append(ref.shortName)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
            )
            ref.longName.takeIf { it.isNotBlank() && it != ref.shortName }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
