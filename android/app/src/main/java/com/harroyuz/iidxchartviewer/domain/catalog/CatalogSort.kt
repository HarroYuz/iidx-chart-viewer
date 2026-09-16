package com.harroyuz.iidxchartviewer.domain.catalog

import java.text.Normalizer
import java.util.Locale

internal enum class CatalogSortDimension { TITLE, VERSION }

internal enum class CatalogSortOrder(val dimension: CatalogSortDimension, val descending: Boolean) {
    TITLE_ASCENDING(CatalogSortDimension.TITLE, false),
    TITLE_DESCENDING(CatalogSortDimension.TITLE, true),
    VERSION_ASCENDING(CatalogSortDimension.VERSION, false),
    VERSION_DESCENDING(CatalogSortDimension.VERSION, true);

    fun withDimension(next: CatalogSortDimension): CatalogSortOrder =
        entries.first { it.dimension == next && it.descending == descending }

    fun reversed(): CatalogSortOrder = entries.first { it.dimension == dimension && it.descending != descending }
}

private data class SortableSong<T>(val song: T, val title: String, val version: Int, val index: Int)

/** Ignore case and character width, preserving punctuation and deterministic ties between versions. */
internal fun <T> sortCatalogSongs(
    songs: List<T>,
    order: CatalogSortOrder,
    title: (T) -> String,
    stableKey: (T) -> String,
    versionOrder: (T) -> Int = { Int.MAX_VALUE },
    textageIndex: (T) -> Int? = { null },
): List<T> {
    val keyed = songs.map { song ->
        SortableSong(song, Normalizer.normalize(title(song).trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT),
            versionOrder(song), textageIndex(song) ?: Int.MAX_VALUE)
    }
    val byTitle = compareBy<SortableSong<T>> { it.title }
        .thenBy { title(it.song) }.thenBy { stableKey(it.song) }
    val ascending = if (order.dimension == CatalogSortDimension.VERSION)
        compareBy<SortableSong<T>> { it.version }.thenBy { it.index }.then(byTitle) else byTitle
    return keyed.sortedWith(if (order.descending) ascending.reversed() else ascending).map { it.song }
}
