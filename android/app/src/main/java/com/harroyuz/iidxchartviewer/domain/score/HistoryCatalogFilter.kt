package com.harroyuz.iidxchartviewer.domain.score

import com.harroyuz.iidxchartviewer.domain.catalog.FavoriteFilter
import com.harroyuz.iidxchartviewer.domain.catalog.songGroupKey
import com.harroyuz.iidxchartviewer.domain.catalog.catalogSongTypes
import com.harroyuz.iidxchartviewer.domain.catalog.matchesCatalogFilters
import com.harroyuz.iidxchartviewer.domain.model.ArcadeStatus
import com.harroyuz.iidxchartviewer.domain.model.BjmMusic
import com.harroyuz.iidxchartviewer.domain.model.IidxChart

/** Filter the historical record's own chart, never another difficulty belonging to the song. */
internal fun matchesHistoryCatalogFilters(
    chart: IidxChart?,
    music: BjmMusic?,
    query: String,
    searchFields: Set<String>,
    versions: Set<String>,
    levels: Set<Int>,
    types: Set<ArcadeStatus>,
    favoriteSongKeys: Set<String> = emptySet(),
    favoriteFilter: FavoriteFilter = FavoriteFilter.ALL,
): Boolean {
    if (!favoriteFilter.matches(chart?.let(::songGroupKey) in favoriteSongKeys)) return false
    val matchesMetadata = chart?.matchesCatalogFilters(versions, levels, types)
        ?: (versions.isEmpty() && levels.isEmpty() && types.containsAll(catalogSongTypes))
    if (!matchesMetadata) return false
    val needle = query.trim()
    if (needle.isEmpty()) return true
    val values = buildList {
        if ("曲名" in searchFields) addAll(listOf(chart?.title, chart?.subtitle, music?.title, music?.plainTitle))
        if ("曲师" in searchFields) addAll(listOf(chart?.composer, music?.artist))
        if ("曲风" in searchFields) addAll(listOf(chart?.genre, music?.genre))
    }
    return values.filterNotNull().joinToString(" ").contains(needle, ignoreCase = true)
}
