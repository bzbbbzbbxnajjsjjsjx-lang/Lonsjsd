package com.example.rss.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Outer corners of a group are round, the corners that touch another row are tight. */
private val OuterRadius = 28.dp
private val InnerRadius = 6.dp

/**
 * The shape of row [index] in a group of [count] rows: the first row rounds its top, the last its
 * bottom, and a lone row is round all around. This is what makes a group read as one surface.
 */
fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) OuterRadius else InnerRadius
    val bottom = if (index == count - 1) OuterRadius else InnerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** A section title above a group of settings. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** The surface behind one settings row. */
@Composable
private fun RowSurface(shape: Shape, content: @Composable () -> Unit) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}


@Composable
private fun RowTitles(title: String, subtitle: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A row with a switch on its trailing edge. */
@Composable
fun SwitchRowContent(
    shape: Shape,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) = RowSurface(shape) {
    Row(
        // The whole row toggles, not only the small switch.
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) { RowTitles(title, subtitle) }
        Spacer(Modifier.size(12.dp))
        Switch(
            modifier = Modifier.testTag("switch:$title"),
            checked = checked,
            onCheckedChange = onChange,
            // On shows a check and off shows a cross inside the thumb, as in Material 3 Expressive switches.
            thumbContent = {
                Icon(
                    imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

/** A row with a title and connected segmented buttons under it. */
@Composable
fun <T> ChoiceRowContent(
    shape: Shape,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) = RowSurface(shape) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) { RowTitles(title, subtitle) }
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(label)
                }
            }
        }
    }
}

/** A row that opens another page, with a chevron. */
@Composable
fun NavRowContent(
    shape: Shape,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) = RowSurface(shape) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) { RowTitles(title, subtitle) }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A row that shows a fixed value, such as the app version. */
@Composable
fun InfoRowContent(shape: Shape, icon: ImageVector, title: String, value: String) = RowSurface(shape) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A page of settings: a column of groups. Every group is one segmented surface, its rows are
 * shaped by [groupShape], and a group with no rows is left out.
 */
@Composable
fun SettingsList(content: SettingsListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
    ) {
        SettingsListScope(this).content()
    }
}

class SettingsListScope internal constructor(private val lazy: LazyListScope) {

    /** A titled group of rows. Rows are declared in [rows]; the group's shapes are computed here. */
    fun group(key: String, header: String? = null, rows: SettingsGroupScope.() -> Unit) {
        val built = SettingsGroupScope().apply(rows).rows
        if (built.isEmpty()) return
        lazy.item(key = "$key#gap") { Spacer(Modifier.height(16.dp)) }
        if (header != null) {
            lazy.item(key = "$key#header") { SectionLabel(header) }
        }
        built.forEachIndexed { index, row ->
            if (index > 0) lazy.item(key = "$key#gap$index") { Spacer(Modifier.height(ListItemDefaults.SegmentedGap)) }
            lazy.item(key = row.key) { row.content(groupShape(index, built.size)) }
        }
    }
}

class SettingsGroupScope internal constructor() {
    internal val rows = mutableListOf<SettingsRowSpec>()

    /** A row drawn in the shape its position in the group calls for. */
    fun row(key: String, content: @Composable (Shape) -> Unit) {
        rows += SettingsRowSpec(key, content)
    }
}

internal class SettingsRowSpec(
    val key: String,
    val content: @Composable (Shape) -> Unit,
)
