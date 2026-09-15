package com.harroyuz.iidxchartviewer.ui.motion

/** A short panel must start below the viewport, not just one panel-height below itself. */
internal fun bottomRevealDistance(viewportHeight: Float, top: Float): Float =
    (viewportHeight - top).coerceAtLeast(0f)

internal fun catalogRevealDistance(viewportHeight: Float, top: Float, height: Float, aboveSelected: Boolean): Float =
    if (aboveSelected) -(top + height).coerceAtLeast(0f) else bottomRevealDistance(viewportHeight, top)
