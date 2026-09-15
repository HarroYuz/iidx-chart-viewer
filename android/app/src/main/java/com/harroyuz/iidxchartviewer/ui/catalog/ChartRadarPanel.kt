package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.domain.model.BjmChartMetadata
import com.harroyuz.iidxchartviewer.domain.model.ChartRadar
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.domain.model.RadarAxis
import com.harroyuz.iidxchartviewer.domain.score.difficultyIndex
import com.harroyuz.iidxchartviewer.ui.components.difficultyName
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.motion.browseReveal
import com.harroyuz.iidxchartviewer.ui.motion.BROWSE_DURATION_MS
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ChartRadarPanel(
    songKey: String,
    charts: List<IidxChart>,
    musicId: Int?,
    mode: String,
    metadata: BjmChartMetadata?,
    loading: Boolean,
    visualEffectsDisabled: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    var selectedId by rememberSaveable(songKey, mode) {
        mutableStateOf(charts.firstOrNull { it.difficulty == "A" }?.id ?: charts.lastOrNull()?.id)
    }
    val selected = charts.firstOrNull { it.id == selectedId } ?: charts.lastOrNull()
    val key = selected?.let { "$musicId:${if (mode == "DP") 1 else 0}:${difficultyIndex(it.difficulty)}" }
    val noteCount = metadata?.noteCounts?.get(key)
    val matchesVersion = selected != null && noteCount != null && noteCount == selected.notes &&
        metadata.version == metadata.noteVersion
    val radar = metadata?.radars?.get(key)?.takeIf { matchesVersion }
    Column(
        Modifier.fillMaxWidth().browseReveal(fromBottom = true).background(CardSurface, MaterialTheme.shapes.medium).padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("谱面详情", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (charts.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    charts.forEachIndexed { index, chart ->
                        SegmentedButton(
                            modifier = Modifier.semantics { contentDescription = "${difficultyName(chart.difficulty)} ${chart.level}" },
                            selected = selected?.id == chart.id,
                            onClick = { selectedId = chart.id },
                            shape = SegmentedButtonDefaults.itemShape(index, charts.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = difficultyColor(chart.difficulty).copy(alpha = .12f),
                                activeContentColor = difficultyColor(chart.difficulty),
                            ),
                        ) { Text(chart.difficulty, fontSize = 11.sp, maxLines = 1) }
                    }
                }
            }
        }
        if (radar != null) {
            RadarDiagram(radar, visualEffectsDisabled)
        } else {
            Text(
                if (loading && metadata == null) "正在加载雷达…" else "暂无雷达数据",
                color = Muted, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 22.dp),
            )
        }
        if (error != null || (metadata == null && !loading)) {
            TextButton(onClick = onRetry, enabled = !loading) { Text(if (loading) "正在重试…" else "重新加载雷达数据") }
        }
    }
}

// Hue families sampled from KONAMI's arena radar selector, adapted for a light background.
// https://p.eagate.573.jp/game/2dx/33/event/arena_mode/index.html
private fun radarColor(axis: RadarAxis): Color = when (axis) {
    RadarAxis.NOTES -> Color(0xFFC62EC8)
    RadarAxis.PEAK -> Color(0xFFBD9400)
    RadarAxis.SCRATCH -> Color(0xFFE45B29)
    RadarAxis.SOFLAN -> Color(0xFF138CAD)
    RadarAxis.CHARGE -> Color(0xFF9162DB)
    RadarAxis.CHORD -> Color(0xFF669500)
}

@Composable
private fun RadarDiagram(radar: ChartRadar, visualEffectsDisabled: Boolean) {
    // This composition survives difficulty changes only while both charts have radar data.
    // A new radar appears at its actual values; rapid changes continue from the current shape.
    val transition = updateTransition(radar, label = "radar-difficulty")
    val animatedValues = RadarAxis.entries.mapIndexed { index, axis ->
        transition.animateFloat(
            transitionSpec = {
                if (visualEffectsDisabled) snap() else tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing)
            },
            label = axis.label,
        ) { it.values[index] / 200f }
    }
    val animatedAccent = transition.animateColor(
        transitionSpec = {
            if (visualEffectsDisabled) snap() else tween(BROWSE_DURATION_MS, easing = FastOutSlowInEasing)
        },
        label = "radar-color",
    ) { radarColor(it.dominantAxis) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, lineHeight = 14.sp)
    val valueStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = "tnum")
    // Reserve both columns independently of the selected values and dominant (bold) axis.
    // Measuring with the current font scale also avoids clipping in larger system fonts.
    val columnWidths = remember(textMeasurer, labelStyle, valueStyle) {
        val weights = listOf(FontWeight.Normal, FontWeight.Bold)
        val labels = weights.maxOf { weight ->
            RadarAxis.entries.maxOf { textMeasurer.measure(it.label, labelStyle.copy(fontWeight = weight)).size.width }
        }
        val values = weights.maxOf { weight ->
            (0..9).maxOf { digit ->
                textMeasurer.measure("$digit$digit$digit.$digit$digit", valueStyle.copy(fontWeight = weight)).size.width
            }
        }
        labels to values
    }
    val labelWidth = with(LocalDensity.current) { columnWidths.first.toDp() }
    val valueWidth = with(LocalDensity.current) { columnWidths.second.toDp() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(188.dp).semantics { contentDescription = "谱面雷达图" }.drawWithCache {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) * .33f
            fun point(index: Int, scale: Float): Offset {
                val angle = -Math.PI / 2 + index * Math.PI / 3
                return center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius * scale
            }
            fun polygon(scales: List<Float>): Path = Path().apply {
                scales.forEachIndexed { index, scale ->
                    val p = point(index, scale)
                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            val grid = (1..4).map { step -> polygon(List(6) { step / 4f }) }
            val endpoints = RadarAxis.entries.indices.map { point(it, 1f) }
            val labels = RadarAxis.entries.mapIndexed { index, axis ->
                val label = textMeasurer.measure(axis.label, TextStyle(
                    color = radarColor(axis), fontSize = 8.sp,
                    fontWeight = if (axis == radar.dominantAxis) FontWeight.Bold else FontWeight.Normal,
                ))
                label to (point(index, 1.32f) - Offset(label.size.width / 2f, label.size.height / 2f))
            }
            val shape = Path()
            val vertices = Array(RadarAxis.entries.size) { Offset.Zero }
            val gridStroke = Stroke(1.dp.toPx())
            val radarStroke = Stroke(2.dp.toPx())
            val dotRadius = 2.5.dp.toPx()
            onDrawBehind {
                grid.forEach { drawPath(it, Muted.copy(alpha = .18f), style = gridStroke) }
                endpoints.forEach { drawLine(Muted.copy(alpha = .15f), center, it, gridStroke.width) }
                labels.forEach { (label, position) -> drawText(label, topLeft = position) }
                // Read animation state only when drawing, preserving cached grid/text layouts.
                val accent = if (visualEffectsDisabled) radarColor(radar.dominantAxis) else animatedAccent.value
                shape.rewind()
                vertices.indices.forEach { index ->
                    val scale = if (visualEffectsDisabled) radar.values[index] / 200f else animatedValues[index].value
                    val vertex = point(index, scale)
                    vertices[index] = vertex
                    if (index == 0) shape.moveTo(vertex.x, vertex.y) else shape.lineTo(vertex.x, vertex.y)
                }
                shape.close()
                drawPath(shape, accent.copy(alpha = .20f))
                drawPath(shape, accent, style = radarStroke)
                vertices.forEach { drawCircle(accent, dotRadius, it) }
            }
        })
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RadarAxis.entries.forEachIndexed { index, axis ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        axis.label, modifier = Modifier.width(labelWidth),
                        color = radarColor(axis), style = labelStyle, maxLines = 1, softWrap = false,
                        fontWeight = if (axis == radar.dominantAxis) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        String.format(Locale.ROOT, "%.2f", radar.values[index]),
                        modifier = Modifier.width(valueWidth),
                        color = Ink, style = valueStyle, maxLines = 1, softWrap = false,
                        fontWeight = if (axis == radar.dominantAxis) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
