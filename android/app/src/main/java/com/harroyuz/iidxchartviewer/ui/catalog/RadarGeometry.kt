package com.harroyuz.iidxchartviewer.ui.catalog

internal const val RADAR_GRID_MAX = 100f
internal const val RADAR_VALUE_MAX = 200f

internal data class RadarGeometry(val gridRadius: Float, val labelRadius: Float)

/** Keep 200-value vertices inside the plot; labels may sit within that envelope. */
internal fun radarGeometry(
    width: Float,
    height: Float,
    labelWidth: Float,
    labelHeight: Float,
    edgePadding: Float,
    dotRadius: Float,
): RadarGeometry {
    val fullRadius = (minOf(width, height) / 2f - edgePadding - dotRadius).coerceAtLeast(0f)
    val gridRadius = fullRadius * RADAR_GRID_MAX / RADAR_VALUE_MAX
    val labelRadius = minOf(
        gridRadius * 1.32f,
        width / 2f - labelWidth / 2f - edgePadding,
        height / 2f - labelHeight / 2f - edgePadding,
    ).coerceAtLeast(0f)
    return RadarGeometry(gridRadius, labelRadius)
}
