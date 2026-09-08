package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.harroyuz.iidxchartviewer.ui.motion.browseSharedBounds
import com.harroyuz.iidxchartviewer.ui.motion.keepSharedSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.harroyuz.iidxchartviewer.ui.theme.Ink
import com.harroyuz.iidxchartviewer.ui.theme.Purple

@Composable
internal fun AppTopBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    menu: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigate, modifier = Modifier.semantics { contentDescription = if (menu) "打开菜单" else "返回" }) {
            Canvas(Modifier.size(24.dp)) {
                val stroke = 2.dp.toPx()
                if (menu) {
                    listOf(.25f, .5f, .75f).forEach { y ->
                        drawLine(Ink, Offset(size.width * .15f, size.height * y), Offset(size.width * .85f, size.height * y), stroke, StrokeCap.Round)
                    }
                } else {
                    drawLine(Ink, Offset(size.width * .75f, size.height * .5f), Offset(size.width * .2f, size.height * .5f), stroke, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .2f, size.height * .5f), Offset(size.width * .5f, size.height * .2f), stroke, StrokeCap.Round)
                    drawLine(Ink, Offset(size.width * .2f, size.height * .5f), Offset(size.width * .5f, size.height * .8f), stroke, StrokeCap.Round)
                }
            }
        }
        Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
internal fun PlayStyleButton(mode: String, onToggle: () -> Unit) {
    Box(Modifier.browseSharedBounds("play-style", stable = true).keepSharedSize().size(width = 72.dp, height = 48.dp)) {
        OutlinedButton(
            onClick = onToggle,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(mode, color = Purple)
        }
    }
}
