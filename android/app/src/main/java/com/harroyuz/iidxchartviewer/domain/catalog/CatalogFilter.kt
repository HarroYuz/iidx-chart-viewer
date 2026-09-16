package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

internal val catalogSongTypes = listOf(ArcadeStatus.CURRENT, ArcadeStatus.CONSUMER_ONLY, ArcadeStatus.DELETED)

internal fun toggleCatalogSongType(selected: Set<ArcadeStatus>, type: ArcadeStatus): Set<ArcadeStatus> {
    val valid = selected.intersect(catalogSongTypes.toSet()).ifEmpty { catalogSongTypes.toSet() }
    if (type !in catalogSongTypes || (type in valid && valid.size == 1)) return valid
    return if (type in valid) valid - type else valid + type
}

internal fun IidxChart.matchesCatalogFilters(
    versions: Set<String>,
    levels: Set<Int>,
    types: Set<ArcadeStatus>,
): Boolean =
    (versions.isEmpty() || version in versions) &&
        (levels.isEmpty() || level in levels) &&
        // Unknown metadata remains visible only when the user has not restricted song types.
        (arcadeStatus in types || (arcadeStatus == ArcadeStatus.UNKNOWN && types.containsAll(catalogSongTypes)))

internal data class CatalogVersionOption(val value: String, val label: String, val order: Int) {
    val abbreviation: String get() = when (order) {
        -1 -> "CS"
        0 -> "sub"
        in 1 until Int.MAX_VALUE -> order.toString()
        else -> value
    }
}

internal fun buildCatalogVersionOptions(charts: List<IidxChart>): List<CatalogVersionOption> =
    charts.filter { it.version.isNotBlank() }.groupBy { it.version }.map { (version, members) ->
        val index = members.firstNotNullOfOrNull { it.textageVersion ?: it.textageUrl?.let(::textageVersionIndex) }
        when {
            version.equals("substream", ignoreCase = true) -> CatalogVersionOption(version, "substream", 0)
            index == 0 || version.equals("Consumer only", ignoreCase = true) || version.equals("CS", ignoreCase = true) ->
                CatalogVersionOption(version, "Consumer Only", -1)
            else -> CatalogVersionOption(
                version,
                if (index != null && index >= 11) "$index $version" else version,
                index ?: versionNumber(version),
            )
        }
    }.sortedWith(compareBy<CatalogVersionOption> { it.order }.thenBy { it.label })
