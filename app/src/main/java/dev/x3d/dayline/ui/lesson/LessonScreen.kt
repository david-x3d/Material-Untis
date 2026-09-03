package dev.x3d.dayline.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.LessonStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    id: Long,
    onBack: () -> Unit,
    viewModel: LessonViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { viewModel.setId(id) }
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.lesson_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
            },
        )
        val item = lesson
        if (item == null) {
            Text(stringResource(R.string.error_generic), modifier = Modifier.padding(24.dp))
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val cancelled = item.status == LessonStatus.CANCELLED
                Text(
                    item.subjectLabel,
                    style = MaterialTheme.typography.displaySmall,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                )
                if (cancelled) Text(stringResource(R.string.status_cancelled), color = MaterialTheme.colorScheme.error)
                if (item.status == LessonStatus.IRREGULAR) {
                    Text(stringResource(R.string.status_irregular), color = MaterialTheme.colorScheme.tertiary)
                }
                Detail(stringResource(R.string.lesson_time), "${item.start.format()} – ${item.end.format()}")
                Detail(stringResource(R.string.lesson_room), item.room?.longName ?: item.roomLabel)
                Detail(stringResource(R.string.lesson_teacher), item.teacher?.longName ?: item.teacherLabel)
                item.klass?.let { Detail(stringResource(R.string.lesson_class), it.longName.ifBlank { it.shortName }) }
                item.substText?.let { Detail(stringResource(R.string.lesson_subst), it) }
                item.info?.let { Detail(stringResource(R.string.lesson_info), it) }
                item.lessonText?.let { Detail(stringResource(R.string.lesson_info), it) }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
