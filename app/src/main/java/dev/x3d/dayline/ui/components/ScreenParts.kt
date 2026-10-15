package dev.x3d.dayline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Top bar shared by the tabs: one large title, optional small subtitle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = actions,
    )
}

/** Small uppercase-free section label above a [SegmentedGroup]. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** Expressive grouped list: big outer corners, small inner ones, 2dp gaps. */
@Composable
fun SegmentedGroup(rows: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, row ->
            val big = 24.dp
            val small = 6.dp
            val shape = RoundedCornerShape(
                topStart = if (index == 0) big else small,
                topEnd = if (index == 0) big else small,
                bottomStart = if (index == rows.lastIndex) big else small,
                bottomEnd = if (index == rows.lastIndex) big else small,
            )
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                row()
            }
        }
    }
}
