package com.harroyuz.iidxchartviewer.ui.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.harroyuz.iidxchartviewer.ui.components.difficultyColor
import com.harroyuz.iidxchartviewer.ui.motion.browseReveal
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
        Modifier.fillMaxWidth().browseReveal().background(CardSurface, MaterialTheme.shapes.medium).padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("谱面雷达", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("点选难度切换雷达", color = Muted, fontSize = 10.sp)
        }
        if (charts.isNotEmpty()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                charts.forEachIndexed { index, chart ->
                    SegmentedButton(
                        selected = selected?.id == chart.id,
                        onClick = { selectedId = chart.id },
                        shape = SegmentedButtonDefaults.itemShape(index, charts.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = difficultyColor(chart.difficulty).copy(alpha = .12f),
                            activeContentColor = difficultyColor(chart.difficulty),
                        ),
                    ) { Text("${chart.difficulty} ${chart.level}", fontSize = 12.sp, maxLines = 1) }
                }
            }
        }
        if (radar != null) {
            RadarDiagram(radar)
            Text("BJM · ${metadata.version} 代 · 各维度满值 200", color = Muted, fontSize = 10.sp)
        } else {
            Text(
                when {
                    loading && metadata == null -> "正在加载雷达…"
                    error != null && metadata == null -> error
                    musicId == null -> "暂无对应的 BJM 曲目，请先同步 BJM 曲目库"
                    metadata != null && metadata.version != metadata.noteVersion -> "当前版本的雷达数据尚未更新"
                    !matchesVersion && noteCount != null -> "谱面版本的 NOTE 数不一致，暂不展示雷达"
                    else -> "此难度暂无雷达数据"
                },
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
private fun RadarDiagram(radar: ChartRadar) {
    val accent = radarColor(radar.dominantAxis)
    val textMeasurer = rememberTextMeasurer()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(188.dp)) {
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
            for (step in 1..4) {
                drawPath(polygon(List(6) { step / 4f }), Muted.copy(alpha = .18f), style = Stroke(1.dp.toPx()))
            }
            RadarAxis.entries.forEachIndexed { index, axis ->
                drawLine(Muted.copy(alpha = .15f), center, point(index, 1f), 1.dp.toPx())
                val label = textMeasurer.measure(axis.label, TextStyle(color = radarColor(axis), fontSize = 8.sp))
                val position = point(index, 1.32f)
                drawText(label, topLeft = position - Offset(label.size.width / 2f, label.size.height / 2f))
            }
            val values = radar.values.map { it / 200f }
            drawPath(polygon(values), accent.copy(alpha = .20f))
            drawPath(polygon(values), accent, style = Stroke(2.dp.toPx()))
            values.forEachIndexed { index, value -> drawCircle(accent, 2.5.dp.toPx(), point(index, value)) }
        }
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RadarAxis.entries.forEachIndexed { index, axis ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(axis.label, color = radarColor(axis), fontSize = 10.sp)
                    Text(
                        "  ${String.format(Locale.ROOT, "%.2f", radar.values[index])}",
                        color = Ink, fontSize = 11.sp,
                        fontWeight = if (axis == radar.dominantAxis) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
