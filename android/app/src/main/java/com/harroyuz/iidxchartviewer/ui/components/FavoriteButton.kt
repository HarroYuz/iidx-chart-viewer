package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.harroyuz.iidxchartviewer.ui.theme.Muted
import com.harroyuz.iidxchartviewer.ui.theme.Purple
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onToggle() },
        modifier = modifier.size(40.dp).semantics {
            contentDescription = if (isFavorite) "取消收藏" else "收藏曲目"
            stateDescription = if (isFavorite) "已收藏" else "未收藏"
        },
    ) {
        Canvas(Modifier.size(24.dp)) {
            val star = Path().apply {
                repeat(10) { index ->
                    val angle = -Math.PI / 2 + index * Math.PI / 5
                    val radius = size.minDimension * if (index % 2 == 0) .45f else .20f
                    val x = center.x + cos(angle).toFloat() * radius
                    val y = center.y + sin(angle).toFloat() * radius
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            if (isFavorite) drawPath(star, Purple)
            else drawPath(star, Muted, style = Stroke(1.7.dp.toPx()))
        }
    }
}
