package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.harroyuz.iidxchartviewer.domain.model.IidxChart
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import com.harroyuz.iidxchartviewer.ui.theme.Background
import com.harroyuz.iidxchartviewer.ui.theme.CardSurface
import com.harroyuz.iidxchartviewer.ui.theme.Muted

/** The same colored surface changes bounds; labels above it can change independently. */
@Composable
internal fun DifficultyBackground(
    chart: IidxChart,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shared: Boolean = true,
    detail: Boolean = false,
) {
    val accent = difficultyColor(chart.difficulty)
    val available = chart.textageUrl != null
    Canvas(modifier.browseSharedBounds(if (shared) "difficulty-color:${chart.id}" else null, overlayZ = .5f)) {
        val radius = CornerRadius((if (detail) 16.dp else 14.dp).toPx())
        drawRoundRect(if (!available) Background else if (detail) CardSurface else accent.copy(alpha = .13f), cornerRadius = radius)
        val width = (if (selected) 2.dp else 1.dp).toPx()
        drawRoundRect(
            color = if (detail) accent.copy(alpha = if (available) .35f else .15f)
                else if (available) accent.copy(alpha = if (selected) .95f else .55f) else Muted.copy(alpha = .7f),
            topLeft = Offset(width / 2, width / 2),
            size = Size((size.width - width).coerceAtLeast(0f), (size.height - width).coerceAtLeast(0f)),
            cornerRadius = CornerRadius((radius.x - width / 2).coerceAtLeast(0f)),
            style = Stroke(width, pathEffect = if (available || detail) null else PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()))),
        )
    }
}
