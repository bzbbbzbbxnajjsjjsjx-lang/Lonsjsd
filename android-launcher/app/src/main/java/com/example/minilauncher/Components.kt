package com.example.minilauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** One row of the app actions sheet. */
class ActionItem(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** Cookie-shaped icon badge, the same one used on the settings rows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconBadge(icon: ImageVector, destructive: Boolean = false) {
    val container = if (destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (destructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A tappable card row: badge, label, same shape rules as the settings cards. */
@Composable
fun ActionRow(
    icon: ImageVector,
    label: String,
    shape: Shape,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, destructive)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Long-press menu for an app, laid out like the settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(app: AppEntry, items: List<ActionItem>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
            )
            items.forEachIndexed { index, item ->
                ActionRow(
                    icon = item.icon,
                    label = item.label,
                    shape = segmentShape(index, items.size),
                    destructive = item.destructive,
                    onClick = item.onClick,
                )
            }
        }
    }
}

/** App name with the typed prefix in bold, so matches stand out in the list. */
fun highlightMatch(label: String, query: String, highlight: Color): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(label)
    val start = label.indexOf(q, ignoreCase = true)
    if (start < 0) return AnnotatedString(label)
    return buildAnnotatedString {
        append(label.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlight)) {
            append(label.substring(start, start + q.length))
        }
        append(label.substring(start + q.length))
    }
}
