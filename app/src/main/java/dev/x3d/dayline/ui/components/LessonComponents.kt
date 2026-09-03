package dev.x3d.dayline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonStatus

@Composable
fun OfflineBanner(visible: Boolean) {
    if (!visible) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            stringResource(R.string.offline_banner),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cancelled = lesson.status == LessonStatus.CANCELLED
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().alpha(if (cancelled) 0.7f else 1f),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = when (lesson.status) {
                LessonStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                LessonStatus.IRREGULAR -> MaterialTheme.colorScheme.tertiaryContainer
                LessonStatus.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${lesson.start.format()} – ${lesson.end.format()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                lesson.subjectLabel,
                style = MaterialTheme.typography.titleLarge,
                textDecoration = if (cancelled) TextDecoration.LineThrough else null,
            )
            Text("${lesson.roomLabel} · ${lesson.teacherLabel}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cancelled) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.status_cancelled)) })
                }
                if (lesson.status == LessonStatus.IRREGULAR) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.status_irregular)) })
                }
            }
            lesson.substText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
