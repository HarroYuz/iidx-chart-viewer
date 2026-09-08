package com.harroyuz.iidxchartviewer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.harroyuz.iidxchartviewer.ui.motion.LocalBrowseMotion

/** Keep visited pages composed, but do not place, draw or expose hidden pages to input. */
@Composable
internal fun RetainedPage(
    visible: Boolean,
    modifier: Modifier = Modifier,
    interactive: Boolean = visible,
    content: @Composable () -> Unit,
) {
    var visited by remember { mutableStateOf(visible) }
    if (visible) visited = true
    if (!visited) return

    CompositionLocalProvider(
        LocalBrowseMotion provides if (visible) LocalBrowseMotion.current else null,
    ) {
        Box(
            modifier.fillMaxSize()
                .then(if (interactive) Modifier else Modifier.clearAndSetSemantics { })
                .then(if (!visible || interactive) Modifier else Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    })
                .layout { measurable, constraints ->
                    val page = measurable.measure(constraints)
                    layout(page.width, page.height) {
                        if (visible) page.placeRelative(0, 0)
                    }
                },
        ) {
            content()
        }
    }
}
