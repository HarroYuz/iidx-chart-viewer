package com.harroyuz.iidxchartviewer.domain.catalog

import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

internal val catalogSongTypes = listOf(ArcadeStatus.DELETED, ArcadeStatus.CONSUMER_ONLY, ArcadeStatus.CURRENT)

internal fun IidxChart.matchesCatalogFilters(
    versions: Set<String>,
    levels: Set<Int>,
    types: Set<ArcadeStatus>,
): Boolean =
    (versions.isEmpty() || version in versions) &&
        (levels.isEmpty() || level in levels) &&
        // Unknown metadata remains visible only when the user has not restricted song types.
        (arcadeStatus in types || (arcadeStatus == ArcadeStatus.UNKNOWN && types.containsAll(catalogSongTypes)))

internal data class CatalogVersionOption(val value: String, val label: String, val order: Int)

internal fun buildCatalogVersionOptions(charts: List<IidxChart>): List<CatalogVersionOption> =
    charts.filter { it.version.isNotBlank() }.groupBy { it.version }.map { (version, members) ->
        val index = members.firstNotNullOfOrNull { it.textageUrl?.let(::textageVersionIndex) }
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
