package com.harroyuz.iidxchartviewer.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harroyuz.iidxchartviewer.ui.theme.*
import kotlin.math.roundToInt

@Composable
internal fun PlayerRotaryOverlay(center: Offset, pointer: Offset, label: String) {
    val density = LocalDensity.current
    val radius = with(density) { 48.dp.toPx() }
    val labelWidth = with(density) { 180.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Panel.copy(alpha = .96f), radius, center)
            drawCircle(Purple.copy(alpha = .6f), radius, center, style = Stroke(2.dp.toPx()))
            drawCircle(Purple.copy(alpha = .12f), 12.dp.toPx(), center)
            val delta = pointer - center
            val knob = center + delta * (radius * .8f / delta.getDistance().coerceAtLeast(radius * .8f))
            drawLine(Purple.copy(alpha = .5f), center, knob, 2.dp.toPx())
            drawCircle(Purple, 9.dp.toPx(), knob)
        }
        Text(
            "$label\n顺时针 ＋ · 逆时针 −",
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset {
                IntOffset((center.x - labelWidth / 2).coerceIn(0f, (constraints.maxWidth - labelWidth).coerceAtLeast(0f)).roundToInt(),
                    (center.y - radius - with(density) { 54.dp.toPx() }).coerceAtLeast(0f).roundToInt())
            }.width(180.dp).background(Panel, RoundedCornerShape(8.dp)).padding(6.dp),
        )
    }
}
