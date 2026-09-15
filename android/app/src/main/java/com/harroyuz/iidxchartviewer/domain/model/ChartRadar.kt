package com.harroyuz.iidxchartviewer.domain.model

/** Clockwise order; values use the same 0–200 scale for every chart. */
enum class RadarAxis(val label: String) {
    NOTES("NOTES"), PEAK("PEAK"), SCRATCH("SCRATCH"),
    SOFLAN("SOF-LAN"), CHARGE("CHARGE"), CHORD("CHORD"),
}

data class ChartRadar(val values: List<Float>) {
    init {
        require(values.size == RadarAxis.entries.size)
        require(values.all { it.isFinite() && it in 0f..200f })
    }
    val available: Boolean get() = values.any { it > 0f }
    // Ties use the fixed clockwise order so the color never flickers.
    val dominantAxis: RadarAxis get() = RadarAxis.entries[values.indices.maxBy { values[it] }]
}

data class BjmChartMetadata(
    val version: Int,
    val radars: Map<String, ChartRadar>,
    val noteCounts: Map<String, Int>,
    val noteVersion: Int = version,
)
