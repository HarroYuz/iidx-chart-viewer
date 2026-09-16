package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.catalog.CatalogVersionOption
import com.harroyuz.iidxchartviewer.domain.catalog.catalogSongTypes
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Outline
import com.harroyuz.iidxchartviewer.ui.theme.Panel
import com.harroyuz.iidxchartviewer.ui.theme.Purple

internal fun ArcadeStatus.filterLabel(): String = when (this) {
    ArcadeStatus.DELETED -> "删除曲"
    ArcadeStatus.CONSUMER_ONLY -> "家用版"
    ArcadeStatus.CURRENT -> "稼动中"
    ArcadeStatus.UNKNOWN -> "未知"
}

@Composable
internal fun CatalogFilterPanel(
    searchFields: List<String>,
    onSearchFieldToggle: (String) -> Unit,
    types: Set<ArcadeStatus>,
    onTypeToggle: (ArcadeStatus) -> Unit,
    levels: Set<Int>,
    onLevelToggle: (Int) -> Unit,
    versions: Set<String>,
    versionOptions: List<CatalogVersionOption>,
    onVersionToggle: (String) -> Unit,
    onClearLevels: () -> Unit,
    onClearVersions: () -> Unit,
    onReset: () -> Unit,
    visualEffectsDisabled: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf("level") }
    fun toggleSection(value: String) {
        expanded = section != value || !expanded
        section = value
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChoiceRow("关键字搜索范围：", listOf("曲名", "曲师", "曲风"), searchFields::contains, onSearchFieldToggle)
        FilterChoiceRow("曲目类型：", catalogSongTypes, types::contains, onTypeToggle) { it.filterLabel() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterExpansionButton(
                "等级", if (levels.isEmpty()) "全部" else levels.sorted().joinToString("/"),
                expanded && section == "level", { toggleSection("level") }, Modifier.weight(1f),
            )
            FilterExpansionButton(
                "版本", if (versions.isEmpty()) "全部" else "${versions.size} 项",
                expanded && section == "version", { toggleSection("version") }, Modifier.weight(1f),
            )
            CompactFilterAction("重置", onClick = onReset)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = if (visualEffectsDisabled) EnterTransition.None else
                expandVertically(tween(180), expandFrom = Alignment.Top) + fadeIn(tween(120)),
            exit = if (visualEffectsDisabled) ExitTransition.None else
                shrinkVertically(tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(100)),
        ) {
            // The bounded, scrollable grid keeps the catalog usable on small screens.
            val maxHeight = minOf(320.dp, (LocalConfiguration.current.screenHeightDp * .38f).dp)
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("可多选，未选时不限", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    CompactFilterAction("不限", onClick = if (section == "level") onClearLevels else onClearVersions)
                }
                if (section == "level") {
                    FilterGrid((1..12).toList(), 4, levels::contains, onLevelToggle, Modifier.heightIn(max = maxHeight)) { it.toString() }
                } else if (section == "version") {
                    FilterGrid(versionOptions, 2, { it.value in versions }, { onVersionToggle(it.value) }, Modifier.heightIn(max = maxHeight)) { it.label }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterChoiceRow(
    title: String,
    choices: List<T>,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    label: (T) -> String = { it.toString() },
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Muted, fontSize = 11.sp, modifier = Modifier.width(100.dp))
        choices.forEach { item ->
            CompactFilterChoice(label(item), isSelected(item), { onToggle(item) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> FilterGrid(
    items: List<T>,
    columns: Int,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    CompactFilterChoice(label(item), isSelected(item), { onToggle(item) }, Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CompactFilterChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.height(32.dp).clip(shape)
            .background(if (selected) Panel else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) Purple.copy(alpha = .6f) else Outline, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Purple else Ink, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FilterExpansionButton(title: String, summary: String, expanded: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.height(32.dp).clip(shape).border(1.dp, if (expanded) Purple else Outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${if (expanded) "收起" else "展开"}${title}筛选" }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$title · $summary", color = Ink, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(if (expanded) "▴" else "▾", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun CompactFilterAction(text: String, onClick: () -> Unit) {
    Box(Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick)
        .padding(PaddingValues(horizontal = 8.dp)), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, fontSize = 11.sp)
    }
}
